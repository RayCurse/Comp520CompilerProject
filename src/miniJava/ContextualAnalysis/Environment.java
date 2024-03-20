package miniJava.ContextualAnalysis;

import java.util.Stack;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Environment {
    private List<Map<String, Declaration>> scopes = new ArrayList<Map<String, Declaration>>();
    private Map<ClassDecl, Map<String, FieldDecl>> classFields = new HashMap<ClassDecl, Map<String, FieldDecl>>();
    private Map<ClassDecl, Map<String, MethodDecl>> classMethods = new HashMap<ClassDecl, Map<String, MethodDecl>>();

    public void openScope() {
        Map<String, Declaration> scope = new HashMap<String, Declaration>();
        scopes.add(scope);
    }
    public void closeScope() {
        scopes.remove(scopes.size() - 1);
    }

    public void addDeclaration(String name, Declaration declaration) {
        Map<String, Declaration> topLevel = scopes.get(scopes.size() - 1);
        topLevel.put(name, declaration);
    }

    public Declaration findDeclaration(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Declaration> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    public void addClass(ClassDecl classDecl, Map<String, FieldDecl> fields, Map<String, MethodDecl> methods) {
        classFields.put(classDecl, fields);
        classMethods.put(classDecl, methods);
    }
}
