package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.util.Set;

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
    private void expectTokenType(TokenType validType) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
    }

    private void expectTokenTypeAndText(TokenType validType, String validStr) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
        if (!token.text.equals(validStr)) {
            throw new TerminalParseException(String.format("Expected %s, got %s in terminal type %s at source position %s", validStr, token.text, token.type, token.getTokenPosition()));
        }
    }
    private void expectTokenTypeAndText(TokenType validType, Set<String> validStrs) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s at source position %s", validType, token.type, token.getTokenPosition()));
        }
        if (!validStrs.contains(token.text)) {
            throw new TerminalParseException(String.format("Expected one of %s, got %s in terminal type %s at source position %s", validStrs, token.text, token.type, token.getTokenPosition()));
        }
    }

    public Parser(Scanner tokenStream) {
        this.tokenStream = tokenStream;
    }

    private Boolean parseResult;
    public boolean parseTokenStream() throws IOException {
        if (parseResult != null) {
            return parseResult;
        }
        parseResult = true;
        try {
            cfg_Program();
        } catch (TerminalParseException e) {
            parseResult = false;
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
    private void cfg_Program() throws IOException, TerminalParseException {
        while (lookAhead().type != TokenType.EOT) {
            cfg_ClassDeclaration();
        }
        expectTokenType(TokenType.EOT);
    }

    private void cfg_ClassDeclaration() throws IOException, TerminalParseException {
        expectTokenType(TokenType.ClassKeyword);
        expectTokenType(TokenType.Id);
        expectTokenType(TokenType.LCurly);
        while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
            cfg_ClassMemberDeclaration();
        }
        expectTokenType(TokenType.RCurly);
    }

    private void cfg_ClassMemberDeclaration() throws IOException, TerminalParseException {
        boolean isMethod = false;
        cfg_Visibility();
        cfg_Access();
        if (lookAhead().type == TokenType.VoidKeyword) {
            // must be method since void only applies to methods
            isMethod = true;
            expectTokenType(TokenType.VoidKeyword);
        } else {
            cfg_Type();
        }
        expectTokenType(TokenType.Id);
        if (!isMethod && lookAhead().type == TokenType.Semicolon) {
            // field
            expectTokenType(TokenType.Semicolon);
        } else {
            // method
            expectTokenType(TokenType.LParen);
            if (lookAhead().type != TokenType.RParen) {
                cfg_ParameterList();
            }
            expectTokenType(TokenType.RParen);
            expectTokenType(TokenType.LCurly);
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                cfg_Statement();
            }
            expectTokenType(TokenType.RCurly);
        }
    }

    private void cfg_Visibility() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.PublicKeyword) {
            expectTokenType(TokenType.PublicKeyword);
        } else if (lookAhead().type == TokenType.PrivateKeyword) {
            expectTokenType(TokenType.PrivateKeyword);
        }
    }

    private void cfg_Access() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.StaticKeyword) {
            expectTokenType(TokenType.StaticKeyword);
        }
    }

    private void cfg_Type() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.IntKeyword || lookAhead().type == TokenType.Id) {
            if (lookAhead().type == TokenType.IntKeyword) {
                expectTokenType(TokenType.IntKeyword);
            } else if (lookAhead().type == TokenType.Id) {
                expectTokenType(TokenType.Id);
            }
            if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                expectTokenType(TokenType.RBrack);
            }
        } else if (lookAhead().type == TokenType.BooleanKeyword) {
            expectTokenType(TokenType.BooleanKeyword);
        }
    }

    private void cfg_ParameterList() throws IOException, TerminalParseException {
        cfg_Type();
        expectTokenType(TokenType.Id);
        while (lookAhead().type == TokenType.Comma) {
            expectTokenType(TokenType.Comma);
            cfg_Type();
            expectTokenType(TokenType.Id);
        }
    }

    private void cfg_ArgumentList() throws IOException, TerminalParseException {
        cfg_Expression();
        while (lookAhead().type == TokenType.Comma) {
            expectTokenType(TokenType.Comma);
            cfg_Expression();
        }
    }

    private void cfg_Reference() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.Id) {
            expectTokenType(TokenType.Id);
        } else {
            expectTokenType(TokenType.ThisKeyword);
        }
        while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
            expectTokenType(TokenType.Dot);
            expectTokenType(TokenType.Id);
        }
    }

    private void cfg_Statement() throws IOException, TerminalParseException {
        boolean isRef = false;
        if (lookAhead().type == TokenType.LCurly) {
            expectTokenType(TokenType.LCurly);
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                cfg_Statement();
            }
            expectTokenType(TokenType.RCurly);
        } else if (lookAhead().type == TokenType.ReturnKeyword) {
            expectTokenType(TokenType.ReturnKeyword);
            if (lookAhead().type != TokenType.Semicolon) {
                cfg_Expression();
            }
            expectTokenType(TokenType.Semicolon);
        } else if (lookAhead().type == TokenType.IfKeyword) {
            expectTokenType(TokenType.IfKeyword);
            expectTokenType(TokenType.LParen);
            cfg_Expression();
            expectTokenType(TokenType.RParen);
            cfg_Statement();
            if (lookAhead().type == TokenType.ElseKeyword) {
                expectTokenType(TokenType.ElseKeyword);
                cfg_Statement();
            }
        } else if (lookAhead().type == TokenType.WhileKeyword) {
            expectTokenType(TokenType.WhileKeyword);
            expectTokenType(TokenType.LParen);
            cfg_Expression();
            expectTokenType(TokenType.RParen);
            cfg_Statement();
        } else if (lookAhead().type == TokenType.Id) {
            int refIdType = cfg_RefOrId();
            if (refIdType == 0) {
                // Is a type, parsed up till Type
                expectTokenType(TokenType.Id);
                expectTokenType(TokenType.Equals);
                cfg_Expression();
                expectTokenType(TokenType.Semicolon);
            } else if (refIdType == 1) {
                // Is a ref, parsed up till Reference
                if (lookAhead().type == TokenType.Equals) {
                    expectTokenType(TokenType.Equals);
                    cfg_Expression();
                    expectTokenType(TokenType.Semicolon);
                } else {
                    expectTokenType(TokenType.LParen);
                    if (lookAhead().type != TokenType.RParen) {
                        cfg_ArgumentList();
                    }
                    expectTokenType(TokenType.RParen);
                    expectTokenType(TokenType.Semicolon);
                }
            } else {
                // Is a ref, parsed up till Reference[Expression]
                expectTokenType(TokenType.Equals);
                cfg_Expression();
                expectTokenType(TokenType.Semicolon);
            }
        } else if (lookAhead().type == TokenType.ThisKeyword) {
            cfg_Reference();
            if (lookAhead().type == TokenType.Equals) {
                expectTokenType(TokenType.Equals);
                cfg_Expression();
                expectTokenType(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                cfg_Expression();
                expectTokenType(TokenType.RBrack);
                expectTokenType(TokenType.Equals);
                cfg_Expression();
                expectTokenType(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.LParen) {
                expectTokenType(TokenType.LParen);
                if (lookAhead().type != TokenType.RParen) {
                    cfg_ArgumentList();
                }
                expectTokenType(TokenType.RParen);
                expectTokenType(TokenType.Semicolon);
            }
        } else {
            cfg_Type();
            expectTokenType(TokenType.Id);
            expectTokenType(TokenType.Equals);
            cfg_Expression();
            expectTokenType(TokenType.Semicolon);
        }
    }
    private int cfg_RefOrId() throws IOException, TerminalParseException {
        // When we expect a type or reference but lookahead was an id
        expectTokenType(TokenType.Id);
        if (lookAhead().type == TokenType.Id) {
            return 0;
        } else if (lookAhead().type == TokenType.LBrack) {
            // Could still be either
            expectTokenType(TokenType.LBrack);
            if (lookAhead().type == TokenType.RBrack) {
                // This is a type
                expectTokenType(TokenType.RBrack);
                return 0;
            } else {
                // This is a reference
                cfg_Expression();
                expectTokenType(TokenType.RBrack);
                return 2;
            }
        } else {
            // This is a reference
            int type = 1;
            while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
                expectTokenType(TokenType.Dot);
                expectTokenType(TokenType.Id);
            }
            if (lookAhead().type == TokenType.LBrack) {
                type = 2;
                expectTokenType(TokenType.LBrack);
                cfg_Expression();
                expectTokenType(TokenType.RBrack);
            }
            return type;
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

    private void cfg_Expression() throws IOException, TerminalParseException {
        cfg_Expression1();
        while (lookAhead().type == TokenType.BinOp && lookAhead().text.equals(op0)) {
            expectTokenTypeAndText(TokenType.BinOp, op0);
            cfg_Expression1();
        }
    }
    private void cfg_Expression1() throws IOException, TerminalParseException {
        cfg_Expression2();
        while (lookAhead().type == TokenType.BinOp && lookAhead().text.equals(op1)) {
            expectTokenTypeAndText(TokenType.BinOp, op1);
            cfg_Expression2();
        }
    }
    private void cfg_Expression2() throws IOException, TerminalParseException {
        cfg_Expression3();
        while (lookAhead().type == TokenType.BinOp && ops2.contains(lookAhead().text)) {
            expectTokenTypeAndText(TokenType.BinOp, ops2);
            cfg_Expression3();
        }
    }
    private void cfg_Expression3() throws IOException, TerminalParseException {
        cfg_Expression4();
        while (lookAhead().type == TokenType.BinOp && ops3.contains(lookAhead().text)) {
            expectTokenTypeAndText(TokenType.BinOp, ops3);
            cfg_Expression4();
        }
    }
    private void cfg_Expression4() throws IOException, TerminalParseException {
        cfg_Expression5();
        while (lookAhead().type == TokenType.BinOp && ops4.contains(lookAhead().text)) {
            expectTokenTypeAndText(TokenType.BinOp, ops4);
            cfg_Expression5();
        }
    }
    private void cfg_Expression5() throws IOException, TerminalParseException {
        cfg_Expression6();
        while (lookAhead().type == TokenType.BinOp && ops5.contains(lookAhead().text)) {
            expectTokenTypeAndText(TokenType.BinOp, ops5);
            cfg_Expression6();
        }
    }
    private void cfg_Expression6() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.UnOp && ops6.contains(lookAhead().text)) {
            expectTokenTypeAndText(TokenType.UnOp, ops6);
            cfg_Expression6();
        } else {
            cfg_Expression7();
        }
    }

    private void cfg_Expression7() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.Id || lookAhead().type == TokenType.ThisKeyword) {
            cfg_Reference();
            if (lookAhead().type == TokenType.LBrack) {
                expectTokenType(TokenType.LBrack);
                cfg_Expression();
                expectTokenType(TokenType.RBrack);
            } else if (lookAhead().type == TokenType.LParen) {
                expectTokenType(TokenType.LParen);
                if (lookAhead().type != TokenType.RParen) {
                    cfg_ArgumentList();
                }
                expectTokenType(TokenType.RParen);
            }
        } else if (lookAhead().type == TokenType.LParen) {
            expectTokenType(TokenType.LParen);
            cfg_Expression();
            expectTokenType(TokenType.RParen);
        } else if (lookAhead().type == TokenType.Num) {
            expectTokenType(TokenType.Num);
        } else if (lookAhead().type == TokenType.TrueKeyword) {
            expectTokenType(TokenType.TrueKeyword);
        } else if (lookAhead().type == TokenType.FalseKeyword) {
            expectTokenType(TokenType.FalseKeyword);
        } else {
            expectTokenType(TokenType.NewKeyword);
            if (lookAhead().type == TokenType.Id) {
                expectTokenType(TokenType.Id);
                if (lookAhead().type == TokenType.LParen) {
                    expectTokenType(TokenType.LParen);
                    expectTokenType(TokenType.RParen);
                } else {
                    expectTokenType(TokenType.LBrack);
                    cfg_Expression();
                    expectTokenType(TokenType.RBrack);
                }
            } else {
                expectTokenType(TokenType.IntKeyword);
                expectTokenType(TokenType.LBrack);
                cfg_Expression();
                expectTokenType(TokenType.RBrack);
            }
        }
    }
}
