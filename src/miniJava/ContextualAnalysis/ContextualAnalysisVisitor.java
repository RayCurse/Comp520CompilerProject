package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Map;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class ContextualAnalysisVisitor implements Visitor<Environment, Void> {

	@Override
	public Void visitPackage(Package prog, Environment env) {
        // Pre populate scopes for level 0 and level 1
        for (ClassDecl classDecl : prog.classDeclList) {
            Map<String, FieldDecl> fields = new HashMap<String, FieldDecl>();
            for (FieldDecl fieldDecl : classDecl.fieldDeclList) {
                fields.put(fieldDecl.name, fieldDecl);
            }

            Map<String, MethodDecl> methods = new HashMap<String, MethodDecl>();
            for (MethodDecl methodDecl : classDecl.methodDeclList) {
                methods.put(methodDecl.name, methodDecl);
            }

            env.addClass(classDecl, fields, methods);
        }

        // Visit all classes
        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitClassDecl(ClassDecl cd, Environment env) {
        for (FieldDecl fieldDecl : cd.fieldDeclList) {
            fieldDecl.visit(this, env);
        }
        for (MethodDecl methodDecl : cd.methodDeclList) {
            methodDecl.visit(this, env);
        }
        return null;
	}

	@Override
	public Void visitFieldDecl(FieldDecl fd, Environment env) {
        return null;
	}

	@Override
	public Void visitMethodDecl(MethodDecl md, Environment env) {
        env.openScope();
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitParameterDecl'");
	}

	@Override
	public Void visitVarDecl(VarDecl decl, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitVarDecl'");
	}

	@Override
	public Void visitBaseType(BaseType type, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBaseType'");
	}

	@Override
	public Void visitClassType(ClassType type, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitClassType'");
	}

	@Override
	public Void visitArrayType(ArrayType type, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitArrayType'");
	}

	@Override
	public Void visitBlockStmt(BlockStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
	}

	@Override
	public Void visitVardeclStmt(VarDeclStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitVardeclStmt'");
	}

	@Override
	public Void visitAssignStmt(AssignStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitAssignStmt'");
	}

	@Override
	public Void visitIxAssignStmt(IxAssignStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIxAssignStmt'");
	}

	@Override
	public Void visitCallStmt(CallStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCallStmt'");
	}

	@Override
	public Void visitReturnStmt(ReturnStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
	}

	@Override
	public Void visitIfStmt(IfStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
	}

	@Override
	public Void visitWhileStmt(WhileStmt stmt, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
	}

	@Override
	public Void visitUnaryExpr(UnaryExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitUnaryExpr'");
	}

	@Override
	public Void visitBinaryExpr(BinaryExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
	}

	@Override
	public Void visitRefExpr(RefExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitRefExpr'");
	}

	@Override
	public Void visitIxExpr(IxExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIxExpr'");
	}

	@Override
	public Void visitCallExpr(CallExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
	}

	@Override
	public Void visitLiteralExpr(LiteralExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitLiteralExpr'");
	}

	@Override
	public Void visitNewObjectExpr(NewObjectExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitNewObjectExpr'");
	}

	@Override
	public Void visitNewArrayExpr(NewArrayExpr expr, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitNewArrayExpr'");
	}

	@Override
	public Void visitThisRef(ThisRef ref, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitThisRef'");
	}

	@Override
	public Void visitIdRef(IdRef ref, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIdRef'");
	}

	@Override
	public Void visitQRef(QualRef ref, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitQRef'");
	}

	@Override
	public Void visitIdentifier(Identifier id, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
	}

	@Override
	public Void visitOperator(Operator op, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
	}

	@Override
	public Void visitIntLiteral(IntLiteral num, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIntLiteral'");
	}

	@Override
	public Void visitBooleanLiteral(BooleanLiteral bool, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBooleanLiteral'");
	}

	@Override
	public Void visitNullLiteral(NullLiteral num, Environment env) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitNullLiteral'");
	}
}
