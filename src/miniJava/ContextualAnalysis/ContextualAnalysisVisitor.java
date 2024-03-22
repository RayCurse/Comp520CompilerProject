package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class ContextualAnalysisVisitor implements Visitor<Environment, Void> {

	@Override
	public Void visitPackage(Package prog, Environment env) {
        // Pre populate scopes for level 0 and level 1
        for (ClassDecl classDecl : prog.classDeclList) {
            env.currentClass = classDecl;
            env.addClass(classDecl);
        }
        for (ClassDecl classDecl : prog.classDeclList) {
            env.currentClass = classDecl;
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                fieldDecl.visit(this, env);
            }
        }
        env.currentClass = null;

        // Visit all classes
        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, env);
        }

        return null;
	}

	@Override
	public Void visitClassDecl(ClassDecl cd, Environment env) {
        env.currentClass = cd;
        for (MethodDecl methodDecl : cd.methodDeclList) {
            env.isStaticContext = methodDecl.isStatic;
            env.openScope();
            methodDecl.visit(this, env);
            env.closeScope();
        }
        env.currentClass = null;
        return null;
	}

	@Override
	public Void visitFieldDecl(FieldDecl fd, Environment env) {
        fd.type.visit(this, env);
        return null;
	}

	@Override
	public Void visitMethodDecl(MethodDecl md, Environment env) {
        md.type.visit(this, env);
        for (ParameterDecl parameterDecl : md.parameterDeclList) {
            parameterDecl.visit(this, env);
        }

        for (Statement statement : md.statementList) {
            statement.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitParameterDecl(ParameterDecl pd, Environment env) {
        env.addDeclaration(pd);
        return null;
	}

	@Override
	public Void visitVarDecl(VarDecl decl, Environment env) {
        decl.type.visit(this, env);
        env.addDeclaration(decl);
        return null;
	}

	@Override
	public Void visitBlockStmt(BlockStmt stmt, Environment env) {
        env.openScope();
        for (Statement statement : stmt.sl) {
            statement.visit(this, env);
        }
        env.closeScope();
        return null;
	}

	@Override
	public Void visitVardeclStmt(VarDeclStmt stmt, Environment env) {
        stmt.varDecl.visit(this, env);
        // type check TODO: check LHS and RHS type are same
        stmt.initExp.visit(this, env);
        return null;
	}

	@Override
	public Void visitAssignStmt(AssignStmt stmt, Environment env) {
        stmt.ref.visit(this, env);
        // type check TODO: check LHS and RHS type are same
        stmt.val.visit(this, env);
        return null;
	}

	@Override
	public Void visitIxAssignStmt(IxAssignStmt stmt, Environment env) {
        stmt.ix.visit(this, env);
        stmt.ref.visit(this, env);
        stmt.exp.visit(this, env);
        // type check TODO: check ix expression is int
        // type check TODO: check LHS and RHS type are same
        return null;
	}

	@Override
	public Void visitCallStmt(CallStmt stmt, Environment env) {
        env.isMethodContext = true;
        stmt.methodRef.visit(this, env);
        env.isMethodContext = false;
        for (Expression expression : stmt.argList) {
            expression.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitReturnStmt(ReturnStmt stmt, Environment env) {
        stmt.returnExpr.visit(this, env);
        return null;
	}

	@Override
	public Void visitIfStmt(IfStmt stmt, Environment env) {
        stmt.cond.visit(this, env);
        stmt.thenStmt.visit(this, env);
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitWhileStmt(WhileStmt stmt, Environment env) {
        stmt.cond.visit(this, env);
        stmt.body.visit(this, env);
        return null;
	}

	@Override
	public Void visitUnaryExpr(UnaryExpr expr, Environment env) {
        expr.expr.visit(this, env);
        // type check TODO: check type
        return null;
	}

	@Override
	public Void visitBinaryExpr(BinaryExpr expr, Environment env) {
        expr.left.visit(this, env);
        expr.right.visit(this, env);
        // type check TODO: check type
        return null;
	}

	@Override
	public Void visitRefExpr(RefExpr expr, Environment env) {
        expr.ref.visit(this, env);
        return null;
	}

	@Override
	public Void visitIxExpr(IxExpr expr, Environment env) {
        expr.ref.visit(this, env);
        expr.ixExpr.visit(this, env);
        // type check TODO: check index is int, check ref is array type
        return null;
	}

	@Override
	public Void visitCallExpr(CallExpr expr, Environment env) {
        env.isMethodContext = true;
        expr.functionRef.visit(this, env);
        env.isMethodContext = false;
        for (Expression expression : expr.argList) {
            expression.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitNewObjectExpr(NewObjectExpr expr, Environment env) {
        // No type checking needed here; it's already a class type
        expr.classtype.visit(this, env);
        return null;
	}

	@Override
	public Void visitNewArrayExpr(NewArrayExpr expr, Environment env) {
        // No type checking needed here; should already be an int or class type from the parsing
        expr.eltType.visit(this, env);
        return null;
	}

	@Override
	public Void visitThisRef(ThisRef ref, Environment env) {
        // type check TODO, check if env is method context
        return null;
	}

	@Override
	public Void visitIdRef(IdRef ref, Environment env) {
        // type check TODO, check if env is method context
        ref.id.declaration = env.findDeclaration(ref.id);
        return null;
	}

	@Override
	public Void visitQRef(QualRef ref, Environment env) {
        boolean isMethodContext = env.isMethodContext;
        env.isMethodContext = false;

        Reference leftRef = ref.ref;
        leftRef.visit(this, env);

        Identifier leftId = null;
        if (leftRef instanceof IdRef) {
            leftId = ((IdRef) leftRef).id;
        } else if (leftRef instanceof QualRef) {
            leftId = ((QualRef) leftRef).id;
        } else {
            // leftRef is a ThisRef
            ref.id.declaration = env.findClassMember(env.currentClass, ref.id, false, isMethodContext);
            if (ref.id.declaration == null) {
                env.errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", ref.id.posn));
            }
        }

        if (leftId != null && leftId.declaration != null) {
            if (leftId.declaration instanceof ClassDecl) {
                // Left identifier is a class name, must resolve this identifier in static context
                MemberDecl declaration = env.findClassMember((ClassDecl) leftId.declaration, ref.id, true, isMethodContext);
                if (declaration == null) {
                    env.errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", ref.id.posn));
                } else if ((ClassDecl) leftId.declaration != env.currentClass) {
                    // Check if private since it's a different class
                    if (declaration.isPrivate) {
                        env.errorMessages.add(String.format("Identification error at %s, identifier is private", ref.id.posn));
                        declaration = null;
                    }
                }
                ref.id.declaration = declaration;
            } else {
                TypeDenoter leftType = leftId.declaration.type;
                if (leftType instanceof ClassType) {
                    // Left identifier is an instance of a class
                    // Non static context
                    ClassDecl classDecl = (ClassDecl) ((ClassType) leftType).className.declaration;
                    if (classDecl != null) {
                        MemberDecl declaration = env.findClassMember(classDecl, ref.id, false, isMethodContext);
                        if (declaration == null) {
                            env.errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", ref.id.posn));
                        } else if (env.currentClass != classDecl) {
                            // Check if private since it's a different class
                            if (declaration.isPrivate) {
                                env.errorMessages.add(String.format("Identification error at %s, identifier is private", ref.id.posn));
                                declaration = null;
                            }
                        }
                        ref.id.declaration = declaration;
                    }
                } else {
                    // Left identifier is a primitive or array type
                    env.errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", ref.id.posn));
                }
            }
        }

        env.isMethodContext = isMethodContext;
        return null;
	}

    @Override
    public Void visitClassType(ClassType type, Environment env) {
        type.className.declaration = env.findClass(type.className);
        if (type.className.declaration == null) {
            env.errorMessages.add(String.format("Identification error at %s, identifier cannot be resolved to a declaration", type.className.posn));
        }
        return null;
    }

    @Override
    public Void visitArrayType(ArrayType type, Environment env) {
        type.eltType.visit(this, env);
        return null;
    }

	@Override public Void visitIdentifier(Identifier id, Environment env) { return null; }
	@Override public Void visitBaseType(BaseType type, Environment env) { return null; }
	@Override public Void visitNullLiteral(NullLiteral num, Environment env) { return null; }
	@Override public Void visitBooleanLiteral(BooleanLiteral bool, Environment env) { return null; }
	@Override public Void visitIntLiteral(IntLiteral num, Environment env) { return null; }
	@Override public Void visitOperator(Operator op, Environment env) { return null; }
	@Override public Void visitLiteralExpr(LiteralExpr expr, Environment env) { return null; }
}
