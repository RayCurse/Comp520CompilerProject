package miniJava.SyntacticAnalyzer;

public class Token {
    final TokenType type;
    final String text;
    public Token(TokenType type, String text) {
        this.type = type;
        this.text = text;
    }
}
