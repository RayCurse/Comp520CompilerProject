package miniJava.ContextualAnalysis;

import java.util.Set;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class ContextualAnalysisVisitor implements Visitor<Environment, Void> {

    // Helper methods
    final private static Set<String> logicalOps = Set.of("&&", "||");
    final private static Set<String> inequalityOps = Set.of(">", ">=", "<", "<=");
    final private static Set<String> arithmeticOps = Set.of("+", "-", "*", "/");
    final private static Set<String> equalityOps = Set.of("==", "!=");

    final private static BaseType intTypeDenoter = new BaseType(TypeKind.INT, null);
    final private static BaseType booleanTypeDenoter = new BaseType(TypeKind.BOOLEAN, null);
    final private static BaseType voidTypeDenoter = new BaseType(TypeKind.VOID, null);
    final private static BaseType errorTypeDenoter = new BaseType(TypeKind.ERROR, null);
    final private static BaseType unsupportedTypeDenoter = new BaseType(TypeKind.UNSUPPORTED, null);

    private static ClassType classTypeDenoter(ClassDecl classDecl) {
        return new ClassType(new Identifier(new Token(TokenType.Id, classDecl.name, null)), null);
    }

    private static TypeDenoter getResultType(TypeDenoter lhs, TypeDenoter rhs, Operator op) {
        // For binary operators
        if (lhs == null || rhs == null) {
            return null;
        }
        if (lhs.typeKind == TypeKind.ERROR || lhs.typeKind == TypeKind.UNSUPPORTED || rhs.typeKind == TypeKind.ERROR || rhs.typeKind == TypeKind.UNSUPPORTED) {
            return errorTypeDenoter;
        } else if (logicalOps.contains(op.spelling)) {
            if (lhs.typeKind == TypeKind.BOOLEAN && rhs.typeKind == TypeKind.BOOLEAN) {
                return booleanTypeDenoter;
            }
        } else if (inequalityOps.contains(op.spelling)) {
            if (lhs.typeKind == TypeKind.INT && rhs.typeKind == TypeKind.INT) {
                return booleanTypeDenoter;
            }
        } else if (arithmeticOps.contains(op.spelling)) {
            if (lhs.typeKind == TypeKind.INT && rhs.typeKind == TypeKind.INT) {
                return intTypeDenoter;
            }
        } else if (equalityOps.contains(op.spelling)) {
            if (lhs.equals(rhs)) {
                return booleanTypeDenoter;
            }
        }
        return errorTypeDenoter;
    }
    private static TypeDenoter getResultType(TypeDenoter valueType, Operator op) {
        // For unary operators
        if (valueType == null) {
            return null;
        }
        if (valueType.typeKind == TypeKind.ERROR || valueType.typeKind == TypeKind.UNSUPPORTED) {
            return errorTypeDenoter;
        } else if (op.spelling.equals("-")) {
            if (valueType.typeKind == TypeKind.INT) {
                return intTypeDenoter;
            }
        } else if (op.spelling.equals("!")) {
            if (valueType.typeKind == TypeKind.BOOLEAN) {
                return booleanTypeDenoter;
            }
        }
        return errorTypeDenoter;
    }

    // Visitor methods
    // Once we visit something, assume that all identifiers in it have declaration field and all expresssions in it have type field
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

        // Make sure we have a main method
        if (env.mainPosition == null) {
            env.errorMessages.add("Context error, program must contain a main method");
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
            if (statement instanceof ReturnStmt) {
                Expression returnExpression = ((ReturnStmt) statement).returnExpr;
                if (returnExpression != null) {
                    if (returnExpression.type != null && returnExpression.type.typeKind != TypeKind.ERROR && !returnExpression.type.equals(md.type)) {
                        env.errorMessages.add(String.format("Type error at %s, invalid return type", statement.posn));
                    }
                } else {
                    if (md.type.typeKind != TypeKind.VOID) {
                        env.errorMessages.add(String.format("Type error at %s, no return value", statement.posn));
                    }
                }
            }
        }

        // Ensure that last statement is a return statement
        if (md.type.typeKind != TypeKind.VOID) {
            if (md.statementList.size() > 0) {
                if (!(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt)) {
                    env.errorMessages.add(String.format("Context error at %s, method must have a return statement", md.posn));
                }
            } else if (md.statementList.size() <= 0) {
                env.errorMessages.add(String.format("Context error at %s, method must have a return statement", md.posn));
            }
        }
        return null;
	}

	@Override
	public Void visitParameterDecl(ParameterDecl pd, Environment env) {
        pd.type.visit(this, env);
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
        // Visit statements
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
        env.currentDeclaringIdentifier = stmt.varDecl.name;

        stmt.initExp.visit(this, env);
        // If identification error in class type in varDecl, then we still report the type error even though we should ignore it
        // Maybe fix this later, but not important
        if (stmt.initExp.type != null && stmt.initExp.type.typeKind != TypeKind.ERROR && !stmt.varDecl.type.equals(stmt.initExp.type)) {
            env.errorMessages.add(String.format("Type error at %s, variable and value type do not match", stmt.posn));
        }

        env.currentDeclaringIdentifier = null;
        return null;
	}

	@Override
	public Void visitAssignStmt(AssignStmt stmt, Environment env) {
        stmt.ref.visit(this, env);
        stmt.val.visit(this, env);
        if (stmt.ref.type != null && stmt.val.type != null && stmt.ref.type.typeKind != TypeKind.ERROR && stmt.val.type.typeKind != TypeKind.ERROR && !stmt.ref.type.equals(stmt.val.type)) {
            env.errorMessages.add(String.format("Type error at %s, variable and value type do not match", stmt.posn));
        }
        return null;
	}

	@Override
	public Void visitIxAssignStmt(IxAssignStmt stmt, Environment env) {
        stmt.ix.visit(this, env);
        stmt.ref.visit(this, env);
        stmt.exp.visit(this, env);

        if (stmt.ref.type != null && stmt.ix.type != null && stmt.exp.type != null) {
            if (stmt.ref.type.typeKind != TypeKind.ARRAY) {
                env.errorMessages.add(String.format("Type error at %s, not indexable", stmt.posn));
            } else if (stmt.ix.type.typeKind != TypeKind.INT) {
                env.errorMessages.add(String.format("Type error at %s, index must be an int", stmt.posn));
            } else if (!stmt.exp.type.equals(((ArrayType) stmt.ref.type).eltType)) {
                env.errorMessages.add(String.format("Type error at %s, array element type and value do not match", stmt.posn));
            }
        }
        return null;
	}

	@Override
	public Void visitCallStmt(CallStmt stmt, Environment env) {
        env.isMethodContext = true;
        stmt.methodRef.visit(this, env);
        env.isMethodContext = false;

        // Get method declaration
        MethodDecl methodDeclaration = null;
        if (stmt.methodRef instanceof ThisRef) {
            env.errorMessages.add(String.format("Type error at %s, invalid method", stmt.posn));
        } else if (stmt.methodRef instanceof IdRef) {
            methodDeclaration = (MethodDecl) ((IdRef) stmt.methodRef).id.declaration;
        } else {
            methodDeclaration = (MethodDecl) ((QualRef) stmt.methodRef).id.declaration;
        }

        // Type check arguments
        if (methodDeclaration != null) {
            if (methodDeclaration.parameterDeclList.size() != stmt.argList.size()) {
                env.errorMessages.add(String.format("Type error at %s, invalid arguments", stmt.posn));
            } else {
                for (int i = 0; i < methodDeclaration.parameterDeclList.size(); i++) {
                    ParameterDecl parameter = methodDeclaration.parameterDeclList.get(i);
                    Expression arg = stmt.argList.get(i);
                    arg.visit(this, env);
                    if (arg.type != null && arg.type.typeKind == TypeKind.ERROR) {
                        break;
                    } else if (arg.type != null && !parameter.type.equals(arg.type)) {
                        env.errorMessages.add(String.format("Type error at %s, invalid arguments", stmt.posn));
                        break;
                    }
                }
            }
        }
        return null;
	}

	@Override
	public Void visitReturnStmt(ReturnStmt stmt, Environment env) {
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitIfStmt(IfStmt stmt, Environment env) {
        stmt.cond.visit(this, env);
        if (stmt.cond.type.typeKind != TypeKind.BOOLEAN) {
            env.errorMessages.add(String.format("Type error at %s, if statement condition must be a boolean", stmt.cond.posn));
        }

        if (stmt.thenStmt instanceof VarDeclStmt) {
            env.errorMessages.add(String.format("Context error at %s, cannot have a scope with only a single declaration statement", stmt.thenStmt.posn));
            return null;
        }
        stmt.thenStmt.visit(this, env);

        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt) {
                env.errorMessages.add(String.format("Context error at %s, cannot have a scope with only a single declaration statement", stmt.elseStmt.posn));
            }
            stmt.elseStmt.visit(this, env);
        }

        return null;
	}

	@Override
	public Void visitWhileStmt(WhileStmt stmt, Environment env) {
        stmt.cond.visit(this, env);
        if (stmt.cond.type.typeKind != TypeKind.BOOLEAN) {
            env.errorMessages.add(String.format("Type error at %s, while statement condition must be a boolean", stmt.cond.posn));
        }

        stmt.body.visit(this, env);
        if (stmt.body instanceof VarDeclStmt) {
            env.errorMessages.add(String.format("Context error at %s, cannot have a scope with only a single declaration statement", stmt.body.posn));
        }

        return null;
	}

	@Override
	public Void visitUnaryExpr(UnaryExpr expr, Environment env) {
        expr.expr.visit(this, env);
        expr.type = getResultType(expr.expr.type, expr.operator);
        if (expr.expr.type != null) {
            if (expr.expr.type.typeKind != TypeKind.ERROR && expr.type.typeKind == TypeKind.ERROR) {
                env.errorMessages.add(String.format("Type error at %s", expr.posn));
            }
        }
        return null;
	}

	@Override
	public Void visitBinaryExpr(BinaryExpr expr, Environment env) {
        expr.left.visit(this, env);
        expr.right.visit(this, env);
        expr.type = getResultType(expr.left.type, expr.right.type, expr.operator);
        if (expr.left.type != null && expr.right.type != null) {
            if ((expr.left.type.typeKind != TypeKind.ERROR && expr.right.type.typeKind != TypeKind.ERROR) && (expr.type.typeKind == TypeKind.ERROR)) {
                env.errorMessages.add(String.format("Type error at %s", expr.posn));
            }
        }
        return null;
	}

	@Override
	public Void visitRefExpr(RefExpr expr, Environment env) {
        expr.ref.visit(this, env);
        if (expr.ref instanceof ThisRef) {
            expr.type = classTypeDenoter(env.currentClass);
        } else if (expr.ref instanceof IdRef) {
            Declaration declaration = ((IdRef) expr.ref).id.declaration;
            if (declaration != null) {
                if (declaration instanceof ClassDecl) {
                    expr.type = unsupportedTypeDenoter; // The class metatype is unsupported
                } else {
                    expr.type = declaration.type;
                }
            }
        } else {
            Declaration declaration = ((QualRef) expr.ref).id.declaration;
            if (declaration != null) {
                expr.type = declaration.type;
            }
        }
        return null;
	}

	@Override
	public Void visitIxExpr(IxExpr expr, Environment env) {
        expr.ref.visit(this, env);
        expr.ixExpr.visit(this, env);
        if (expr.ref.type == null || expr.ixExpr.type == null) {
            expr.type = null;
        } else if (expr.ref.type.typeKind != TypeKind.ARRAY) {
            env.errorMessages.add(String.format("Type error at %s, not indexable", expr.posn));
            expr.type = errorTypeDenoter;
        } else if (expr.ixExpr.type.typeKind != TypeKind.INT) {
            env.errorMessages.add(String.format("Type error at %s, index must be an int", expr.posn));
            expr.type = errorTypeDenoter;
        } else {
            expr.type = ((ArrayType) expr.ref.type).eltType;
        }
        return null;
	}

	@Override
	public Void visitCallExpr(CallExpr expr, Environment env) {
        env.isMethodContext = true;
        expr.functionRef.visit(this, env);
        env.isMethodContext = false;

        // Get method declaration
        MethodDecl methodDeclaration = null;
        if (expr.functionRef instanceof ThisRef) {
            env.errorMessages.add(String.format("Type error at %s, invalid method", expr.posn));
        } else if (expr.functionRef instanceof IdRef) {
            methodDeclaration = (MethodDecl) ((IdRef) expr.functionRef).id.declaration;
        } else {
            methodDeclaration = (MethodDecl) ((QualRef) expr.functionRef).id.declaration;
        }

        // Type check arguments
        if (methodDeclaration != null) {
            boolean argsCorrect = true;
            if (methodDeclaration.parameterDeclList.size() != expr.argList.size()) {
                argsCorrect = false;
                env.errorMessages.add(String.format("Type error at %s, invalid arguments", expr.posn));
            } else {
                for (int i = 0; i < methodDeclaration.parameterDeclList.size(); i++) {
                    ParameterDecl parameter = methodDeclaration.parameterDeclList.get(i);
                    Expression arg = expr.argList.get(i);
                    arg.visit(this, env);
                    if (arg.type != null && arg.type.typeKind == TypeKind.ERROR) {
                        argsCorrect = false;
                        break;
                    } else if (arg.type != null && !parameter.type.equals(arg.type)) {
                        env.errorMessages.add(String.format("Type error at %s, invalid arguments", expr.posn));
                        argsCorrect = false;
                        break;
                    }
                }
            }
            if (argsCorrect) {
                expr.type = methodDeclaration.type;
            } else {
                expr.type = errorTypeDenoter;
            }
        }

        return null;
	}

	@Override
	public Void visitNewObjectExpr(NewObjectExpr expr, Environment env) {
        // No type checking needed here; it's already a class type
        expr.classtype.visit(this, env);
        expr.type = expr.classtype;
        return null;
	}

	@Override
	public Void visitNewArrayExpr(NewArrayExpr expr, Environment env) {
        expr.eltType.visit(this, env);
        expr.sizeExpr.visit(this, env);
        if (expr.sizeExpr.type != null) {
            if (expr.sizeExpr.type.typeKind != TypeKind.INT) {
                env.errorMessages.add(String.format("Type error at %s, size must be an int", expr.posn));
            } else {
                expr.type =  new ArrayType(expr.eltType, null);
            }
        }
        return null;
	}

	@Override
	public Void visitThisRef(ThisRef ref, Environment env) {
        if (env.isStaticContext) {
            env.errorMessages.add(String.format("Context error at %s, cannot refer to \"this\" in a static context", ref.posn));
        }
        return null;
	}

	@Override
	public Void visitIdRef(IdRef ref, Environment env) {
        ref.id.declaration = env.findDeclaration(ref.id);
        if (ref.id.declaration != null) {
            ref.type = ref.id.declaration.type;
            if (ref.id.spelling.equals(env.currentDeclaringIdentifier)) {
                env.errorMessages.add(String.format("Context error at %s, cannot use variable name \"%s\" in its own declaration", ref.id.posn, env.currentDeclaringIdentifier));
            }
        }
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

        if (ref.id.declaration != null) {
            ref.type = ref.id.declaration.type;
        }
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

    @Override public Void visitLiteralExpr(LiteralExpr expr, Environment env) {
        if (expr.lit instanceof NullLiteral) {
            expr.type = voidTypeDenoter;
        } else if (expr.lit instanceof BooleanLiteral) {
            expr.type = booleanTypeDenoter;
        } else if (expr.lit instanceof IntLiteral) {
            expr.type = intTypeDenoter;
        }
        // expr.lit instanceof Identifier also possible from the typing, but not actually possible since we never used LiteralExpr with Identifier
        // stand alone identifiers are taken care of by visitRefExpr

        return null;
    }

	@Override public Void visitNullLiteral(NullLiteral num, Environment env) { return null; }
	@Override public Void visitBooleanLiteral(BooleanLiteral bool, Environment env) { return null; }
	@Override public Void visitIntLiteral(IntLiteral num, Environment env) { return null; }
	@Override public Void visitOperator(Operator op, Environment env) { return null; }
	@Override public Void visitIdentifier(Identifier id, Environment env) { return null; }
	@Override public Void visitBaseType(BaseType type, Environment env) { return null; }
}
