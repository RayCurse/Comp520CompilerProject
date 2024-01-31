package miniJava.SyntacticAnalyzer;

import java.io.IOException;

public class Parser {
    public boolean printTokens = false;
    private Scanner tokenStream;
    private Token lookAheadBuffer = null;
    private Token lookAhead() throws IOException, TerminalParseException {
        if (lookAheadBuffer == null) {
            lookAheadBuffer = tokenStream.scan();
            if (lookAheadBuffer == null) {
                throw new TerminalParseException(String.format("Invalid token"));
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
                throw new TerminalParseException(String.format("Invalid token"));
            }
        }
        if (printTokens) {
            System.out.print(res);
        }
        return res;
    }
    private void expectToken(TokenType validType) throws IOException, TerminalParseException {
        Token token = nextToken();
        if (token.type != validType) {
            throw new TerminalParseException(String.format("Expected %s, got %s", validType, token.type));
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
        expectToken(TokenType.EOT);
    }

    private void cfg_ClassDeclaration() throws IOException, TerminalParseException {
        expectToken(TokenType.ClassKeyword);
        expectToken(TokenType.Id);
        expectToken(TokenType.LCurly);
        while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
            cfg_ClassMemberDeclaration();
        }
        expectToken(TokenType.RCurly);
    }

    private void cfg_ClassMemberDeclaration() throws IOException, TerminalParseException {
        boolean isMethod = false;
        cfg_Visibility();
        cfg_Access();
        if (lookAhead().type == TokenType.VoidKeyword) {
            // must be method since void only applies to methods
            isMethod = true;
            expectToken(TokenType.VoidKeyword);
        } else {
            cfg_Type();
        }
        expectToken(TokenType.Id);
        if (!isMethod && lookAhead().type == TokenType.Semicolon) {
            // field
            expectToken(TokenType.Semicolon);
        } else {
            // method
            expectToken(TokenType.LParen);
            if (lookAhead().type != TokenType.RParen) {
                cfg_ParameterList();
            }
            expectToken(TokenType.RParen);
            expectToken(TokenType.LCurly);
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                cfg_Statement();
            }
            expectToken(TokenType.RCurly);
        }
    }

    private void cfg_Visibility() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.PublicKeyword) {
            expectToken(TokenType.PublicKeyword);
        } else if (lookAhead().type == TokenType.PrivateKeyword) {
            expectToken(TokenType.PrivateKeyword);
        }
    }

    private void cfg_Access() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.StaticKeyword) {
            expectToken(TokenType.StaticKeyword);
        }
    }

    private void cfg_Type() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.IntKeyword || lookAhead().type == TokenType.Id) {
            if (lookAhead().type == TokenType.IntKeyword) {
                expectToken(TokenType.IntKeyword);
            } else if (lookAhead().type == TokenType.Id) {
                expectToken(TokenType.Id);
            }
            if (lookAhead().type == TokenType.LBrack) {
                expectToken(TokenType.LBrack);
                expectToken(TokenType.RBrack);
            }
        } else if (lookAhead().type == TokenType.BooleanKeyword) {
            expectToken(TokenType.BooleanKeyword);
        }
    }

    private void cfg_ParameterList() throws IOException, TerminalParseException {
        cfg_Type();
        expectToken(TokenType.Id);
        while (lookAhead().type == TokenType.Comma) {
            expectToken(TokenType.Comma);
            cfg_Type();
            expectToken(TokenType.Id);
        }
    }

    private void cfg_ArgumentList() throws IOException, TerminalParseException {
        cfg_Expression();
        while (lookAhead().type == TokenType.Comma) {
            expectToken(TokenType.Comma);
            cfg_Expression();
        }
    }

    private void cfg_Reference() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.Id) {
            expectToken(TokenType.Id);
        } else {
            expectToken(TokenType.ThisKeyword);
        }
        while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
            expectToken(TokenType.Dot);
            expectToken(TokenType.Id);
        }
    }

    private void cfg_Statement() throws IOException, TerminalParseException {
        boolean isRef = false;
        if (lookAhead().type == TokenType.LCurly) {
            expectToken(TokenType.LCurly);
            while (lookAhead().type != TokenType.RCurly && lookAhead().type != TokenType.EOT) {
                cfg_Statement();
            }
            expectToken(TokenType.RCurly);
        } else if (lookAhead().type == TokenType.ReturnKeyword) {
            expectToken(TokenType.ReturnKeyword);
            if (lookAhead().type != TokenType.Semicolon) {
                cfg_Expression();
            }
            expectToken(TokenType.Semicolon);
        } else if (lookAhead().type == TokenType.IfKeyword) {
            expectToken(TokenType.IfKeyword);
            expectToken(TokenType.LParen);
            cfg_Expression();
            expectToken(TokenType.RParen);
            cfg_Statement();
            if (lookAhead().type == TokenType.ElseKeyword) {
                expectToken(TokenType.ElseKeyword);
                cfg_Statement();
            }
        } else if (lookAhead().type == TokenType.WhileKeyword) {
            expectToken(TokenType.WhileKeyword);
            expectToken(TokenType.LParen);
            cfg_Expression();
            expectToken(TokenType.RParen);
            cfg_Statement();
        } else if (lookAhead().type == TokenType.Id) {
            cfg_RefOrId();
            if (lookAhead().type == TokenType.Id) {
                expectToken(TokenType.Id);
                expectToken(TokenType.Equals);
                cfg_Expression();
                expectToken(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.Equals) {
                expectToken(TokenType.Equals);
                cfg_Expression();
                expectToken(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.RBrack) {
                expectToken(TokenType.RBrack);
                expectToken(TokenType.Equals);
                cfg_Expression();
                expectToken(TokenType.Semicolon);
            } else {
                expectToken(TokenType.LParen);
                if (lookAhead().type != TokenType.RParen) {
                    cfg_ArgumentList();
                }
                expectToken(TokenType.RParen);
                expectToken(TokenType.Semicolon);
            }
        } else if (lookAhead().type == TokenType.ThisKeyword) {
            cfg_Reference();
            if (lookAhead().type == TokenType.Equals) {
                expectToken(TokenType.Equals);
                cfg_Expression();
                expectToken(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.LBrack) {
                expectToken(TokenType.LBrack);
                cfg_Expression();
                expectToken(TokenType.RBrack);
                expectToken(TokenType.Equals);
                cfg_Expression();
                expectToken(TokenType.Semicolon);
            } else if (lookAhead().type == TokenType.LParen) {
                expectToken(TokenType.LParen);
                if (lookAhead().type != TokenType.RParen) {
                    cfg_ArgumentList();
                }
                expectToken(TokenType.RParen);
                expectToken(TokenType.Semicolon);
            }
        } else {
            cfg_Type();
            expectToken(TokenType.Id);
            expectToken(TokenType.Equals);
            cfg_Expression();
            expectToken(TokenType.Semicolon);
        }
    }
    private void cfg_RefOrId() throws IOException, TerminalParseException {
        // When we expect a type or reference but lookahead was an id
        expectToken(TokenType.Id);
        if (lookAhead().type == TokenType.Id) {
            // This is a type and we are done
        } else if (lookAhead().type == TokenType.LBrack) {
            // Could still be either
            expectToken(TokenType.LBrack);
            if (lookAhead().type == TokenType.RBrack) {
                // This is a type
                expectToken(TokenType.RBrack);
            } else {
                // This is a reference
                cfg_Expression();
            }
        } else {
            // This is a reference
            while (lookAhead().type == TokenType.Dot && lookAhead().type != TokenType.EOT) {
                expectToken(TokenType.Dot);
                expectToken(TokenType.Id);
            }
        }
    }

    private void cfg_Expression() throws IOException, TerminalParseException {
        if (lookAhead().type == TokenType.Id || lookAhead().type == TokenType.ThisKeyword) {
            cfg_Reference();
            if (lookAhead().type == TokenType.LBrack) {
                expectToken(TokenType.LBrack);
                cfg_Expression();
                expectToken(TokenType.RBrack);
            } else if (lookAhead().type == TokenType.LParen) {
                expectToken(TokenType.LParen);
                if (lookAhead().type != TokenType.RParen) {
                    cfg_ArgumentList();
                }
                expectToken(TokenType.RParen);
            }
        } else if (lookAhead().type == TokenType.UnOp) {
            expectToken(TokenType.UnOp);
            cfg_Expression();
        } else if (lookAhead().type == TokenType.LParen) {
            expectToken(TokenType.LParen);
            cfg_Expression();
            expectToken(TokenType.RParen);
        } else if (lookAhead().type == TokenType.Num) {
            expectToken(TokenType.Num);
        } else if (lookAhead().type == TokenType.TrueKeyword) {
            expectToken(TokenType.TrueKeyword);
        } else if (lookAhead().type == TokenType.FalseKeyword) {
            expectToken(TokenType.FalseKeyword);
        } else {
            expectToken(TokenType.NewKeyword);
            if (lookAhead().type == TokenType.Id) {
                expectToken(TokenType.Id);
                if (lookAhead().type == TokenType.LParen) {
                    expectToken(TokenType.LParen);
                    expectToken(TokenType.RParen);
                } else {
                    expectToken(TokenType.LBrack);
                    cfg_Expression();
                    expectToken(TokenType.RBrack);
                }
            } else {
                expectToken(TokenType.IntKeyword);
                expectToken(TokenType.LBrack);
                cfg_Expression();
                expectToken(TokenType.RBrack);
            }
        }
        while (lookAhead().type == TokenType.BinOp) {
            expectToken(TokenType.BinOp);
            cfg_Expression();
        }
    }
}
