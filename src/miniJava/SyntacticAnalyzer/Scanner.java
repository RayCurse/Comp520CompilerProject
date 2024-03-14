package miniJava.SyntacticAnalyzer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scanner {
    private FileInputStream fStream;
    private List<Integer> lookAheadBuffer = new ArrayList<Integer>();
    private int currentLineNumber = 1;
    private int currentColumnNumber = 1;
    private int lookAhead(int n) throws IOException {
        if (lookAheadBuffer.size() - 1 >= n) {
            return lookAheadBuffer.get(n);
        } else {
            for (int i = lookAheadBuffer.size(); i <= n; i++) {
                lookAheadBuffer.add(fStream.read());
            }
            return lookAheadBuffer.get(n);
        }
    }
    private int nextChar() throws IOException {
        int res;
        if (lookAheadBuffer.size() > 0) {
            res = lookAheadBuffer.remove(0);
        } else {
            res = fStream.read();
        }
        if ((char) res == '\n') {
            currentLineNumber += 1;
            currentColumnNumber = 1;
        } else {
            currentColumnNumber += 1;
        }
        return res;
    }

    private boolean isWhitespace(int n) {
        char c = (char) n;
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private boolean isNumeric(int n) {
        char c = (char) n;
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(int n) {
        char c = (char) n;
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isValidIdChar(int n) {
        return isNumeric(n) || isAlpha(n) || (char) n == '_';
    }

    private boolean consumeWhitespaceAndComments() throws IOException {
        while (true) {
            boolean foundIgnoredChars = false;

            // Line comment
            if ((char) lookAhead(0) == '/' && (char) lookAhead(1) == '/') {
                foundIgnoredChars = true;
                int currentChar = nextChar();
                while ((char) currentChar != '\n' && (char) currentChar != '\r' && currentChar != -1) {
                    currentChar = nextChar();
                }

            // Block comment
            } else if ((char) lookAhead(0) == '/' && (char) lookAhead(1) == '*') {
                foundIgnoredChars = true;
                nextChar();
                nextChar();
                while (!((char) lookAhead(0) == '*' && (char) lookAhead(1) == '/') && lookAhead(0) != -1) {
                    nextChar();
                }
                if (lookAhead(0) == -1) {
                    // Unclosed block comment
                    return false;
                }
                nextChar();
                nextChar();

            // Whitespace
            } else if (isWhitespace(lookAhead(0))) {
                foundIgnoredChars = true;
                while (isWhitespace(lookAhead(0))) {
                    nextChar();
                }
            }
            if (!foundIgnoredChars) { break; }
        }
        return true;
    }


    public Scanner(FileInputStream fStream) {
        this.fStream = fStream;
    }

    public SourcePosition getCurrentPosition() {
        return new SourcePosition(currentLineNumber, currentColumnNumber);
    }

    private TokenType prevTokenType = null;
    public Token scan() throws IOException {
        if (!consumeWhitespaceAndComments()) {
            return null;
        }
        StringBuilder tokenText = new StringBuilder();

        int currentChar = nextChar();
        tokenText.append((char) currentChar);
        String nextTwoChars = String.valueOf((char) currentChar) + (char) lookAhead(0);

        // EOT token
        if (currentChar == -1) {
            prevTokenType = TokenType.EOT;
            return new Token(TokenType.EOT, new SourcePosition(currentLineNumber, currentColumnNumber - 1));

        // Double char operators
        } else if (doubleCharTokenTypes.containsKey(nextTwoChars)) {
            tokenText.append((char) nextChar());
            prevTokenType = doubleCharTokenTypes.get(nextTwoChars);
            return new Token(prevTokenType, tokenText.toString(), new SourcePosition(currentLineNumber, currentColumnNumber - 2));

        // Single char tokens and single character operators
        } else if (singleCharTokenTypes.containsKey((char) currentChar)) {
            // minus operator is a special case because it may also be a UnOp
            // assume that minus is BinOp and treat is as UnOp if previous token was one that can come before UnOp
            // these tokens are disjoint with those that can come before a BinOp so there is no ambiguity
            if (currentChar == '-' && prevTokenType != null && ValidUnOpPrecedingTokens.contains(prevTokenType)) {
                prevTokenType = TokenType.UnOp;
                return new Token(TokenType.UnOp, tokenText.toString(), new SourcePosition(currentLineNumber, currentColumnNumber - 1));
            }

            prevTokenType = singleCharTokenTypes.get((char) currentChar);
            return new Token(prevTokenType, tokenText.toString(), new SourcePosition(currentLineNumber, currentColumnNumber - 1));

        // Id and keyword tokens
        } else if (isAlpha(currentChar)) {
            SourcePosition pos = new SourcePosition(currentLineNumber, currentColumnNumber - 1);
            while (isValidIdChar(lookAhead(0))) {
                tokenText.append((char) nextChar());
            }
            String text = tokenText.toString();
            if (keywordTokenTypes.containsKey(text)) {
                prevTokenType = keywordTokenTypes.get(text);
                return new Token(prevTokenType, text, pos);
            }
            prevTokenType= TokenType.Id;
            return new Token(prevTokenType, text, pos);

        // Num token
        } else if (isNumeric(currentChar)) {
            while (isNumeric(lookAhead(0))) {
                tokenText.append((char) nextChar());
            }
            prevTokenType = TokenType.Num;
            return new Token(prevTokenType, tokenText.toString(), new SourcePosition(currentLineNumber, currentColumnNumber));
        }
        return null;
    }

    // Grouping tokens by how they can be parsed
    private static Map<Character, TokenType> singleCharTokenTypes = Map.ofEntries(
        Map.entry('(', TokenType.LParen),
        Map.entry(')', TokenType.RParen),
        Map.entry('[', TokenType.LBrack),
        Map.entry(']', TokenType.RBrack),
        Map.entry('{', TokenType.LCurly),
        Map.entry('}', TokenType.RCurly),
        Map.entry('=', TokenType.Equals),
        Map.entry('.', TokenType.Dot),
        Map.entry(',', TokenType.Comma),
        Map.entry(';', TokenType.Semicolon),

        Map.entry('>', TokenType.BinOp),
        Map.entry('<', TokenType.BinOp),
        Map.entry('+', TokenType.BinOp),
        Map.entry('-', TokenType.BinOp),
        Map.entry('*', TokenType.BinOp),
        Map.entry('/', TokenType.BinOp),
        Map.entry('!', TokenType.UnOp)
    );

    private static Map<String, TokenType> doubleCharTokenTypes = Map.ofEntries(
        Map.entry("==", TokenType.BinOp),
        Map.entry("<=", TokenType.BinOp),
        Map.entry(">=", TokenType.BinOp),
        Map.entry("!=", TokenType.BinOp),
        Map.entry("&&", TokenType.BinOp),
        Map.entry("||", TokenType.BinOp)
    );

    private static Map<String, TokenType> keywordTokenTypes = Map.ofEntries(
        Map.entry("class", TokenType.ClassKeyword),
        Map.entry("void", TokenType.VoidKeyword),
        Map.entry("public", TokenType.PublicKeyword),
        Map.entry("private", TokenType.PrivateKeyword),
        Map.entry("static", TokenType.StaticKeyword),
        Map.entry("int", TokenType.IntKeyword),
        Map.entry("boolean", TokenType.BooleanKeyword),
        Map.entry("this", TokenType.ThisKeyword),
        Map.entry("return", TokenType.ReturnKeyword),
        Map.entry("if", TokenType.IfKeyword),
        Map.entry("else", TokenType.ElseKeyword),
        Map.entry("while", TokenType.WhileKeyword),
        Map.entry("true", TokenType.TrueKeyword),
        Map.entry("false", TokenType.FalseKeyword),
        Map.entry("new", TokenType.NewKeyword),
        Map.entry("null", TokenType.NullKeyword)
    );

    // from examining the grammar, only the following tokens may come before a UnOp in a valid string:
    // Comma, Equals, LBrack, ReturnKeyword, LParen, UnOp, BinOp
    private static Set<TokenType> ValidUnOpPrecedingTokens = Set.of(
        TokenType.Comma,
        TokenType.Equals,
        TokenType.LBrack,
        TokenType.ReturnKeyword,
        TokenType.LParen,
        TokenType.UnOp,
        TokenType.BinOp
    );
}
