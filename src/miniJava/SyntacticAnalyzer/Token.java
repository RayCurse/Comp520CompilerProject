package miniJava.SyntacticAnalyzer;

public class Token {
    public final TokenType type;
    public final String text;
    public Token(TokenType type, String text) {
        this.type = type;
        this.text = text;
    }
    public Token(TokenType type) {
        this.type = type;
        this.text = null;
    }

    public String toString() {
        if (this.text != null && this.text.length() > 0) {
            return String.format("(%s, \"%s\")", this.type, this.text);
        } else {
            return String.format("(%s)", this.type);
        }
    }
}
