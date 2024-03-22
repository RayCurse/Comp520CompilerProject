package miniJava.ContextualAnalysis;

import java.util.Stack;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Environment {
    private List<Map<String, Declaration>> scopes = new ArrayList<Map<String, Declaration>>();

    // Level 0 scope
    private Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();

    // Level 1 scopes
    // Fields and methods can have the same name
    // Non static and static members can have the same name
    private Map<ClassDecl, Map<String, FieldDecl>> classFields = new HashMap<ClassDecl, Map<String, FieldDecl>>();
    private Map<ClassDecl, Map<String, MethodDecl>> classMethods = new HashMap<ClassDecl, Map<String, MethodDecl>>();
    private Map<ClassDecl, Map<String, FieldDecl>> staticClassFields = new HashMap<ClassDecl, Map<String, FieldDecl>>();
    private Map<ClassDecl, Map<String, MethodDecl>> staticClassMethods = new HashMap<ClassDecl, Map<String, MethodDecl>>();

    private <T extends Declaration> void addToScope(Map<String, T> scope, T declaration) {
        String name = declaration.name;
        if (scope.containsKey(name)) {
            errorMessages.add(String.format("Identification error at %s, identifier \"%s\" already declared at %s", declaration.posn, name, scope.get(name).posn));
            return;
        }
        scope.put(declaration.name, declaration);
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
        Map<ClassDecl, Map<String, FieldDecl>> fields;
        Map<ClassDecl, Map<String, MethodDecl>> methods;
        if (isStaticContext) {
            fields = staticClassFields;
            methods = staticClassMethods;
        } else {
            fields = classFields;
            methods = classMethods;
        }
        if (isMethodContext) {
            if (methods.get(classDecl).containsKey(name)) {
                return methods.get(classDecl).get(name);
            }
        } else {
            if (fields.get(classDecl).containsKey(name)) {
                return fields.get(classDecl).get(name);
            }
        }
        return null;
    }

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
            if (fieldDecl.isStatic) {
                addToScope(staticFields, fieldDecl);
            } else {
                addToScope(fields, fieldDecl);
            }
        }

        for (MethodDecl methodDecl : classDecl.methodDeclList) {
            if (methodDecl.isStatic) {
                staticMethods.put(methodDecl.name, methodDecl);
            } else {
                methods.put(methodDecl.name, methodDecl);
            }
        }
        classFields.put(classDecl, fields);
        classMethods.put(classDecl, methods);
        staticClassFields.put(classDecl, staticFields);
        staticClassMethods.put(classDecl, staticMethods);
    }

    // Other states to keep track of for the visitor
    public ClassDecl currentClass = null;
    public boolean isStaticContext = false;
    public boolean isMethodContext = false;
    public List<String> errorMessages = new ArrayList<String>();
}
