package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

public class Parser {
    public boolean printTokens = false;
    private Scanner tokenStream;
    private Token lookAheadBuffer = null;
    private Token lookAhead() throws IOException, TerminalParseException {
        if (lookAheadBuffer == null) {
            lookAheadBuffer = tokenStream.scan();
            if (lookAheadBuffer == null) {
                throw new TerminalParseException(String.format("Invalid token at source position %s", tokenStream.getCurrentPosition()));
            }
        }
        return lookAheadBuffer;
    }
    private Token nextToken() throws IOException, TerminalParseException {
        Token res;
        if (lookAheadBuffer != null) {
            res = lookAheadBuffer;
            lookAheadBuffer = null;
        } else {
            res = tokenStream.scan();
            if (res == null) {
                throw new TerminalParseException(String.format("Invalid token at source position %s", tokenStream.getCurrentPosition()));
            }
        }
        if (printTokens) {
            System.out.print(res);
        }
        return res;
    }
    private Token expectTokenType(TokenType validType) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
        return token;
    }

    private Token expectTokenTypeAndText(TokenType validType, String validStr) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
        if (!token.text.equals(validStr)) {
            throw new TerminalParseException(String.format("Expected %s, got %s in terminal type %s at source position %s", validStr, token.text, token.type, token.getTokenPosition()));
        }
        return token;
    }
    private Token expectTokenTypeAndText(TokenType validType, Set<String> validStrs) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
        if (!validStrs.contains(token.text)) {
            throw new TerminalParseException(String.format("Expected one of %s, got %s in terminal type %s at source position %s", validStrs, token.text, token.type, token.getTokenPosition()));
        }
        return token;
    }

    public Parser(Scanner tokenStream) {
        this.tokenStream = tokenStream;
    }

    private boolean completedParse = false;
    private Package parseResult;
    public Package parseTokenStream() throws IOException {
        if (completedParse) {
            return parseResult;
        }
        completedParse = true;
        try {
            parseResult = cfg_Program();
        } catch (TerminalParseException e) {
            parseResult = null;
            StackTraceElement[] stackTrace = e.getStackTrace();
            StackTraceElement firstElement = stackTrace[1];
            StringBuilder errorMessageBuilder = new StringBuilder();
            errorMessageBuilder.append(e.getMessage());
            errorMessageBuilder.append('\n');
            errorMessageBuilder.append("while parsing ");
            errorMessageBuilder.append(firstElement.getMethodName().substring(4));
            errorMessageBuilder.append(':');
            errorMessageBuilder.append(firstElement.getLineNumber());
            if (stackTrace.length > 4) {
                errorMessageBuilder.append(" in\n");
                for (int i = 2; i < stackTrace.length - 2; i++) {
                    StackTraceElement element = stackTrace[i];
                    errorMessageBuilder.append("  ");
                    errorMessageBuilder.append(element.getMethodName().substring(4));
                    errorMessageBuilder.append(':');
                    errorMessageBuilder.append(element.getLineNumber());
                    if (i < stackTrace.length - 3) {
                        errorMessageBuilder.append('\n');
                    }
                }
            }
            errorMessage = errorMessageBuilder.toString();
        }
        if (printTokens) {
            System.out.println();
        }
        return parseResult;
    }

    private String errorMessage;
    public String getErrorMsg() {
        return errorMessage;
    }

    // CFG rules
    private Package cfg_Program() throws IOException, TerminalParseException {
        ClassDeclList classList = new ClassDeclList();
        SourcePosition pos = null;
        while (lookAhead().type != TokenType.EOT) {
            ClassDecl classDecl = cfg_ClassDeclaration();
            if (pos == null) {
                pos = classDecl.posn;
            }
            classList.add(classDecl);
        }
        Token eotToken = expectTokenType(TokenType.EOT);
        if (pos == null) {
            pos = eotToken.getTokenPosition();
        }
        return new Package(classList, pos);
    }

    private ClassDecl cfg_ClassDeclaration() throws IOException, TerminalParseException {
        SourcePosition pos = expectTokenType(TokenType.ClassKeyword).getTokenPosition();
        String className = expectTokenType(TokenType.Id).text;
        FieldDeclList fields = new FieldDeclList();
        MethodDeclList methods = new MethodDeclList();
        expectTokenType(TokenType.LCurly);
        while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
            MemberDecl member = cfg_ClassMemberDeclaration();
            if (member instanceof FieldDecl) {
                fields.add((FieldDecl) member);
            } else if (member instanceof MethodDecl) {
                methods.add((MethodDecl) member);
            }
        }
        expectTokenType(TokenType.RCurly);
        return new ClassDecl(className, fields, methods, pos);
    }

    private MemberDecl cfg_ClassMemberDeclaration() throws IOException, TerminalParseException {
        boolean isMethod = false;
        Token visibilityToken = cfg_Visibility();
        Boolean isPrivate = visibilityToken.text.equals("private");
        Boolean isStatic = false;
        TypeDenoter type = null;
        SourcePosition pos = visibilityToken.getTokenPosition();
        if (cfg_Access() != null) {
            isStatic = true;
        }
        if (lookAhead().type == TokenType.VoidKeyword) {
            // must be method since void only applies to methods
            isMethod = true;
            SourcePosition typePos = expectTokenType(TokenType.VoidKeyword).getTokenPosition();
            type = new BaseType(TypeKind.VOID, typePos);
        } else {
            type = cfg_Type();
        }
        String name = expectTokenType(TokenType.Id).text;
        FieldDecl fieldDecl = new FieldDecl(isPrivate, isStatic, type, name, pos);
        if (!isMethod && lookAhead().type == TokenType.Semicolon) {
            // field
            expectTokenType(TokenType.Semicolon);
            return fieldDecl;
        } else {
            // method
            ParameterDeclList params = null;
            StatementList statements = new StatementList();
            expectTokenType(TokenType.LParen);
            if (lookAhead().type != TokenType.RParen) {
                params = cfg_ParameterList();
            } else {
                params = new ParameterDeclList();
            }
            expectTokenType(TokenType.RParen);
            expectTokenType(TokenType.LCurly);
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                statements.add(cfg_Statement());
            }
            expectTokenType(TokenType.RCurly);
            return new MethodDecl(fieldDecl, params, statements, fieldDecl.posn);
        }
    }

    private Token cfg_Visibility() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.PublicKeyword) {
            return expectTokenType(TokenType.PublicKeyword);
        } else {
            return expectTokenType(TokenType.PrivateKeyword);
        }
    }

    private Token cfg_Access() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.StaticKeyword) {
            return expectTokenType(TokenType.StaticKeyword);
        }
        return null;
    }

    private TypeDenoter cfg_Type() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.IntKeyword || lookAhead().type == TokenType.Id) {
            TypeDenoter type = null;
            if (lookAhead().type == TokenType.IntKeyword) {
                type = new BaseType(TypeKind.INT, lookAhead().getTokenPosition());
                expectTokenType(TokenType.IntKeyword);
            } else if (lookAhead().type == TokenType.Id) {
                Identifier classIdentifier = new Identifier(expectTokenType(TokenType.Id));
                type = new ClassType(classIdentifier, classIdentifier.posn);
            }
            if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                expectTokenType(TokenType.RBrack);
                return new ArrayType(type, type.posn);
            }
            return type;
        } else {
            SourcePosition pos = expectTokenType(TokenType.BooleanKeyword).getTokenPosition();
            return new BaseType(TypeKind.BOOLEAN, pos);
        }
    }

    private ParameterDeclList cfg_ParameterList() throws IOException, TerminalParseException {
        ParameterDeclList params = new ParameterDeclList();
        TypeDenoter type = cfg_Type();
        String name = expectTokenType(TokenType.Id).text;
        params.add(new ParameterDecl(type, name, type.posn));
        while (lookAhead().type == TokenType.Comma) {
            expectTokenType(TokenType.Comma);
            type = cfg_Type();
            name = expectTokenType(TokenType.Id).text;
            params.add(new ParameterDecl(type, name, type.posn));
        }
        return params;
    }

    private ExprList cfg_ArgumentList() throws IOException, TerminalParseException {
        ExprList exprs = new ExprList();
        exprs.add(cfg_Expression());
        while (lookAhead().type == TokenType.Comma) {
            expectTokenType(TokenType.Comma);
            exprs.add(cfg_Expression());
        }
        return exprs;
    }

    private Reference cfg_Reference() throws IOException, TerminalParseException {
        Reference ref = null;
        if (lookAhead().type == TokenType.Id) {
            Identifier identifierToken = new Identifier(expectTokenType(TokenType.Id));
            ref = new IdRef(identifierToken, identifierToken.posn);
        } else {
            Token thisToken = expectTokenType(TokenType.ThisKeyword);
            ref = new ThisRef(thisToken.getTokenPosition());
        }
        while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
            expectTokenType(TokenType.Dot);
            Identifier identifier = new Identifier(expectTokenType(TokenType.Id));
            ref = new QualRef(ref, identifier, identifier.posn);
        }
        return ref;
    }

    private Statement cfg_Statement() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.LCurly) {
            SourcePosition pos = expectTokenType(TokenType.LCurly).getTokenPosition();
            StatementList statements = new StatementList();
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                statements.add(cfg_Statement());
            }
            expectTokenType(TokenType.RCurly);
            return new BlockStmt(statements, pos);
        } else if (lookAhead().type == TokenType.ReturnKeyword) {
            SourcePosition pos = expectTokenType(TokenType.ReturnKeyword).getTokenPosition();
            Expression expression = null;
            if (lookAhead().type != TokenType.Semicolon) {
                expression = cfg_Expression();
            }
            expectTokenType(TokenType.Semicolon);
            return new ReturnStmt(expression, pos);
        } else if (lookAhead().type == TokenType.IfKeyword) {
            SourcePosition pos = expectTokenType(TokenType.IfKeyword).getTokenPosition();
            expectTokenType(TokenType.LParen);
            Expression condition = cfg_Expression();
            expectTokenType(TokenType.RParen);
            Statement thenStatement = cfg_Statement();
            Statement elseStatement = null;
            if (lookAhead().type == TokenType.ElseKeyword) {
                expectTokenType(TokenType.ElseKeyword);
                elseStatement = cfg_Statement();
            }
            if (elseStatement != null) {
                return new IfStmt(condition, thenStatement, elseStatement, pos);
            } else {
                return new IfStmt(condition, thenStatement, pos);
            }
        } else if (lookAhead().type == TokenType.WhileKeyword) {
            SourcePosition pos = expectTokenType(TokenType.WhileKeyword).getTokenPosition();
            expectTokenType(TokenType.LParen);
            Expression condition = cfg_Expression();
            expectTokenType(TokenType.RParen);
            Statement statement = cfg_Statement();
            return new WhileStmt(condition, statement, pos);
        } else if (lookAhead().type == TokenType.Id) {
            List<AST> parsedElements = cfg_RefOrId();
            if (parsedElements.get(0) instanceof TypeDenoter) {
                // Is a type, parsed up till Type
                TypeDenoter type = (TypeDenoter) parsedElements.get(0);
                String name = expectTokenType(TokenType.Id).text;
                expectTokenType(TokenType.Equals);
                Expression expression = cfg_Expression();
                expectTokenType(TokenType.Semicolon);
                return new VarDeclStmt(new VarDecl(type, name, type.posn), expression, type.posn);
            } else if (parsedElements.size() == 1) {
                // Is a ref without an index, parsed up till Reference
                Reference reference = (Reference) parsedElements.get(0);
                if (lookAhead().type == TokenType.Equals) {
                    expectTokenType(TokenType.Equals);
                    Expression expression = cfg_Expression();
                    expectTokenType(TokenType.Semicolon);
                    return new AssignStmt(reference, expression, reference.posn);
                } else {
                    expectTokenType(TokenType.LParen);
                    ExprList args = null;
                    if (lookAhead().type != TokenType.RParen) {
                        args = cfg_ArgumentList();
                    }
                    expectTokenType(TokenType.RParen);
                    expectTokenType(TokenType.Semicolon);
                    return new CallStmt(reference, args, reference.posn);
                }
            } else {
                // Is a ref with an index, parsed up till Reference[Expression]
                Reference LHSRef = (Reference) parsedElements.get(0);
                Expression indexExpression = (Expression) parsedElements.get(1);
                expectTokenType(TokenType.Equals);
                Expression RHSExpression = cfg_Expression();
                expectTokenType(TokenType.Semicolon);
                return new IxAssignStmt(LHSRef, indexExpression, RHSExpression, LHSRef.posn);
            }
        } else if (lookAhead().type == TokenType.ThisKeyword) {
            Reference LHSRef = cfg_Reference();
            if (lookAhead().type == TokenType.Equals) {
                expectTokenType(TokenType.Equals);
                Expression expression = cfg_Expression();
                expectTokenType(TokenType.Semicolon);
                return new AssignStmt(LHSRef, expression, LHSRef.posn);
            } else if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                Expression indexExpression = cfg_Expression();
                expectTokenType(TokenType.RBrack);
                expectTokenType(TokenType.Equals);
                Expression RHSExpression = cfg_Expression();
                expectTokenType(TokenType.Semicolon);
                return new IxAssignStmt(LHSRef, indexExpression, RHSExpression, LHSRef.posn);
            } else {
                expectTokenType(TokenType.LParen);
                ExprList args = null;
                if (lookAhead().type != TokenType.RParen) {
                    args = cfg_ArgumentList();
                }
                expectTokenType(TokenType.RParen);
                expectTokenType(TokenType.Semicolon);
                return new CallStmt(LHSRef, args, LHSRef.posn);
            }
        } else {
            TypeDenoter type = cfg_Type();
            String name = expectTokenType(TokenType.Id).text;
            expectTokenType(TokenType.Equals);
            Expression expression = cfg_Expression();
            expectTokenType(TokenType.Semicolon);
            return new VarDeclStmt(new VarDecl(type, name, type.posn), expression, type.posn);
        }
    }
    private List<AST> cfg_RefOrId() throws IOException, TerminalParseException {
        // When we expect a type or reference but lookahead was an id
        List<AST> parsedElements = new ArrayList<AST>(2);
        Token idToken = expectTokenType(TokenType.Id);
        if (lookAhead().type == TokenType.Id) {
            // Is a type
            parsedElements.add(new ClassType(new Identifier(idToken), idToken.getTokenPosition()));
            return parsedElements;
        } else if (lookAhead().type == TokenType.LBrack) {
            // Could still be either
            expectTokenType(TokenType.LBrack);
            if (lookAhead().type == TokenType.RBrack) {
                // This is a type
                expectTokenType(TokenType.RBrack);
                parsedElements.add(new ArrayType(new ClassType(new Identifier(idToken), idToken.getTokenPosition()), idToken.getTokenPosition()));
                return parsedElements;
            } else {
                // This is a reference
                Expression indexExpression = cfg_Expression();
                parsedElements.add(new IdRef(new Identifier(idToken), idToken.getTokenPosition()));
                parsedElements.add(indexExpression);
                expectTokenType(TokenType.RBrack);
                return parsedElements;
            }
        } else {
            // This is a reference
            Reference ref = new IdRef(new Identifier(idToken), idToken.getTokenPosition());
            while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
                expectTokenType(TokenType.Dot);
                ref = new QualRef(ref, new Identifier(expectTokenType(TokenType.Id)), ref.posn);
            }
            parsedElements.add(ref);
            if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                Expression indexExpression = cfg_Expression();
                parsedElements.add(indexExpression);
                expectTokenType(TokenType.RBrack);
            }
            return parsedElements;
        }
    }

    // cfg_ExpressionN represents an expression containing only operations of precedence level N or higher (higher means evaled first)
    // cfg_Expression is 0 and higher
    String op0 = "||";
    String op1 = "&&";
    Set<String> ops2 = Set.of("==", "!=");
    Set<String> ops3 = Set.of("<=", "<", ">", ">=");
    Set<String> ops4 = Set.of("+", "-");
    Set<String> ops5 = Set.of("*", "/");
    Set<String> ops6 = Set.of("-", "!");
    // precedence 7 is everything else

    private Expression cfg_Expression() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression1();
        while (lookAhead().type == TokenType.BinOp && lookAhead().text.equals(op0)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, op0));
            expression = new BinaryExpr(operator, expression, cfg_Expression1(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression1() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression2();
        while (lookAhead().type == TokenType.BinOp && lookAhead().text.equals(op1)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, op1));
            expression = new BinaryExpr(operator, expression, cfg_Expression2(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression2() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression3();
        while (lookAhead().type == TokenType.BinOp && ops2.contains(lookAhead().text)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, ops2));
            expression = new BinaryExpr(operator, expression, cfg_Expression3(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression3() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression4();
        while (lookAhead().type == TokenType.BinOp && ops3.contains(lookAhead().text)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, ops3));
            expression = new BinaryExpr(operator, expression, cfg_Expression4(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression4() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression5();
        while (lookAhead().type == TokenType.BinOp && ops4.contains(lookAhead().text)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, ops4));
            expression = new BinaryExpr(operator, expression, cfg_Expression5(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression5() throws IOException, TerminalParseException {
        Expression expression = cfg_Expression6();
        while (lookAhead().type == TokenType.BinOp && ops5.contains(lookAhead().text)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.BinOp, ops5));
            expression = new BinaryExpr(operator, expression, cfg_Expression6(), expression.posn);
        }
        return expression;
    }
    private Expression cfg_Expression6() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.UnOp && ops6.contains(lookAhead().text)) {
            Operator operator = new Operator(expectTokenTypeAndText(TokenType.UnOp, ops6));
            return new UnaryExpr(operator, cfg_Expression6(), operator.posn);
        } else {
            return cfg_Expression7();
        }
    }

    private Expression cfg_Expression7() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.Id || lookAhead().type == TokenType.ThisKeyword) {
            Reference reference = cfg_Reference();
            if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                Expression indexExpression = cfg_Expression();
                expectTokenType(TokenType.RBrack);
                return new IxExpr(reference, indexExpression, reference.posn);
            } else if (lookAhead().type == TokenType.LParen) {
                expectTokenType(TokenType.LParen);
                ExprList args = null;
                if (lookAhead().type != TokenType.RParen) {
                    args = cfg_ArgumentList();
                }
                expectTokenType(TokenType.RParen);
                return new CallExpr(reference, args, reference.posn);
            } else {
                return new RefExpr(reference, reference.posn);
            }
        } else if (lookAhead().type == TokenType.LParen) {
            expectTokenType(TokenType.LParen);
            Expression expression = cfg_Expression();
            expectTokenType(TokenType.RParen);
            return expression;
        } else if (lookAhead().type == TokenType.Num) {
            Token numToken = expectTokenType(TokenType.Num);
            return new LiteralExpr(new IntLiteral(numToken), numToken.getTokenPosition());
        } else if (lookAhead().type == TokenType.TrueKeyword) {
            Token trueToken = expectTokenType(TokenType.TrueKeyword);
            return new LiteralExpr(new BooleanLiteral(trueToken), trueToken.getTokenPosition());
        } else if (lookAhead().type == TokenType.FalseKeyword) {
            Token falseToken = expectTokenType(TokenType.FalseKeyword);
            return new LiteralExpr(new BooleanLiteral(falseToken), falseToken.getTokenPosition());
        } else {
            SourcePosition pos = expectTokenType(TokenType.NewKeyword).getTokenPosition();
            if (lookAhead().type == TokenType.Id) {
                Token idToken = expectTokenType(TokenType.Id);
                if (lookAhead().type == TokenType.LParen) {
                    expectTokenType(TokenType.LParen);
                    expectTokenType(TokenType.RParen);
                    return new NewObjectExpr(new ClassType(new Identifier(idToken), idToken.getTokenPosition()), pos);
                } else {
                    expectTokenType(TokenType.LBrack);
                    Expression indexExpression = cfg_Expression();
                    expectTokenType(TokenType.RBrack);
                    return new NewArrayExpr(new ClassType(new Identifier(idToken), idToken.getTokenPosition()), indexExpression, pos);
                }
            } else {
                Token intToken = expectTokenType(TokenType.IntKeyword);
                expectTokenType(TokenType.LBrack);
                Expression indexExpression = cfg_Expression();
                expectTokenType(TokenType.RBrack);
                return new NewArrayExpr(new BaseType(TypeKind.INT, intToken.getTokenPosition()), indexExpression, pos);
            }
        }
    }
}
