package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Environment {
    private List<Map<String, Declaration>> scopes = new ArrayList<Map<String, Declaration>>();

    // Level 0 scope
    private Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();

    // Level 1 scopes
    // Fields and methods can have the same name in normal Java
    // Non static and static members can have the same name in normal Java
    private Map<ClassDecl, Map<String, FieldDecl>> classFields = new HashMap<ClassDecl, Map<String, FieldDecl>>();
    private Map<ClassDecl, Map<String, MethodDecl>> classMethods = new HashMap<ClassDecl, Map<String, MethodDecl>>();
    private Map<ClassDecl, Map<String, FieldDecl>> staticClassFields = new HashMap<ClassDecl, Map<String, FieldDecl>>();
    private Map<ClassDecl, Map<String, MethodDecl>> staticClassMethods = new HashMap<ClassDecl, Map<String, MethodDecl>>();

    // Initially implemented where static/non-static and field/method could have same name
    // Later found out that MiniJava doesn't allow duplicates in any case, so added quick fix to addClass method
    private <T extends Declaration> void addToScope(Map<String, T> scope, T declaration) {
        String name = declaration.name;
        if (scope.containsKey(name)) {
            errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", declaration.posn, name, scope.get(name).posn));
            return;
        }
        scope.put(declaration.name, declaration);
    }

    // Init System, _PrintStream, and String in constructor
    public Environment() {
        MethodDeclList printStreamMethods = new MethodDeclList();
        ParameterDeclList printlnParams = new ParameterDeclList();
        printlnParams.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        printStreamMethods.add(new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null), printlnParams, new StatementList(), null));
        ClassDecl printStream = new ClassDecl("_PrintStream", new FieldDeclList(), printStreamMethods, null);

        FieldDeclList systemFields = new FieldDeclList();
        Identifier printStreamIdentifier = new Identifier(new Token(TokenType.Id, "_PrintStream", null));
        printStreamIdentifier.declaration = printStream;
        systemFields.add(new FieldDecl(false, true, new ClassType(printStreamIdentifier, null), "out", null));
        ClassDecl system = new ClassDecl("System", systemFields, new MethodDeclList(), null);

        ClassDecl string = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);

        currentClass = system;
        addClass(system);
        currentClass = printStream;
        addClass(printStream);
        currentClass = string;
        addClass(string);
    }

    // Local var scopes
    public void openScope() {
        Map<String, Declaration> scope = new HashMap<String, Declaration>();
        scopes.add(scope);
    }
    public void closeScope() {
        scopes.remove(scopes.size() - 1);
    }

    public void addDeclaration(Declaration declaration) {
        Map<String, Declaration> topLevel = scopes.get(scopes.size() - 1);
        for (Map<String, Declaration> scope : scopes) {
            if (scope.containsKey(declaration.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", declaration.posn, declaration.name, scope.get(declaration.name).posn));
            }
        }
        addToScope(topLevel, declaration);
    }

    // Find decl in current scope stack or level 0, level 1 scopes
    public Declaration findDeclaration(Identifier id) {
        String name = id.spelling;

        // Look for local declarations on the stack
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Declaration> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        // Look for members of the current class
        Declaration classMemberDeclaration = findClassMember(currentClass, id, isStaticContext, isMethodContext);
        if (classMemberDeclaration != null) {
            return classMemberDeclaration;
        }

        // Look for class names
        if (classes.containsKey(name)) {
            return classes.get(name);
        }

        // Could not find declaration
        errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", id.posn));
        return null;
    }

    // Find decl in a level 1 scope (class member)
    public MemberDecl findClassMember(ClassDecl classDecl, Identifier id, boolean isStaticContext, boolean isMethodContext) {
        String name = id.spelling;

        if (!isStaticContext) {
            // If not static, first try instance members, then try static members
            if (isMethodContext) {
                if (classMethods.get(classDecl).containsKey(name)) {
                    return classMethods.get(classDecl).get(name);
                }
                if (staticClassMethods.get(classDecl).containsKey(name)) {
                    return staticClassMethods.get(classDecl).get(name);
                }
            } else {
                if (classFields.get(classDecl).containsKey(name)) {
                    return classFields.get(classDecl).get(name);
                }
                if (staticClassFields.get(classDecl).containsKey(name)) {
                    return staticClassFields.get(classDecl).get(name);
                }
            }
        } else {
            // If static, only try static members
            if (isMethodContext) {
                if (staticClassMethods.get(classDecl).containsKey(name)) {
                    return staticClassMethods.get(classDecl).get(name);
                }
            } else {
                if (staticClassFields.get(classDecl).containsKey(name)) {
                    return staticClassFields.get(classDecl).get(name);
                }
            }
        }

        return null;
    }

    // Find decl in level 0 scope
    public ClassDecl findClass(Identifier id) {
        String name = id.spelling;
        if (classes.containsKey(name)) {
            return classes.get(name);
        }
        return null;
    }

    // Add all class names, their fields and methods to level 0 and level 1 scopes initially
    public void addClass(ClassDecl classDecl) {
        if (classes.containsKey(classDecl.name)) {
            errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", classDecl.posn, classDecl.name, classes.get(classDecl.name).posn));
        }
        classes.put(classDecl.name, classDecl);
        Map<String, FieldDecl> fields = new HashMap<String, FieldDecl>();
        Map<String, MethodDecl> methods = new HashMap<String, MethodDecl>();
        Map<String, FieldDecl> staticFields = new HashMap<String, FieldDecl>();
        Map<String, MethodDecl> staticMethods = new HashMap<String, MethodDecl>();

        for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
            // For MiniJava, no duplicate name allowed in any case
            // Use this if statement to report error on duplicates in this case
            if (fields.containsKey(fieldDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", fieldDecl.posn, fieldDecl.name, fields.get(fieldDecl.name).posn));
            } else if (methods.containsKey(fieldDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", fieldDecl.posn, fieldDecl.name, methods.get(fieldDecl.name).posn));
            } else if (staticFields.containsKey(fieldDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", fieldDecl.posn, fieldDecl.name, staticFields.get(fieldDecl.name).posn));
            } else if (staticMethods.containsKey(fieldDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", fieldDecl.posn, fieldDecl.name, staticMethods.get(fieldDecl.name).posn));
            }

            if (fieldDecl.isStatic) {
                addToScope(staticFields, fieldDecl);
            } else {
                addToScope(fields, fieldDecl);
            }
        }

        for (MethodDecl methodDecl : classDecl.methodDeclList) {
            // For MiniJava, no duplicate name allowed in any case
            // Use this if statement to report error on duplicates in this case
            if (fields.containsKey(methodDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", methodDecl.posn, methodDecl.name, fields.get(methodDecl.name).posn));
            } else if (methods.containsKey(methodDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", methodDecl.posn, methodDecl.name, methods.get(methodDecl.name).posn));
            } else if (staticFields.containsKey(methodDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", methodDecl.posn, methodDecl.name, staticFields.get(methodDecl.name).posn));
            } else if (staticMethods.containsKey(methodDecl.name)) {
                errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", methodDecl.posn, methodDecl.name, staticMethods.get(methodDecl.name).posn));
            }
            if (methodDecl.isStatic) {
                addToScope(staticMethods, methodDecl);
            } else {
                addToScope(methods, methodDecl);
            }
        }
        classFields.put(classDecl, fields);
        classMethods.put(classDecl, methods);
        staticClassFields.put(classDecl, staticFields);
        staticClassMethods.put(classDecl, staticMethods);
    }

    // Other states to keep track of for the visitor
    public ClassDecl currentClass = null;
    public boolean isStaticContext = false; // are we in a static method (this is not for QualRefs)
    public boolean isMethodContext = false; // are we trying to find a method (this is needed since I initially implemented it where methods and fields could have the same name)
    public List<String> errorMessages = new ArrayList<String>();
}
