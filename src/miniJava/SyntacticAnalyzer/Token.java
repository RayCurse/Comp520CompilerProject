package miniJava.SyntacticAnalyzer;

public class Token {
    public final TokenType type;
    public final String text;
    private final SourcePosition position;
    public Token(TokenType type, String text, SourcePosition position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }
    public Token(TokenType type, SourcePosition position) {
        this.type = type;
        this.text = null;
        this.position = position;
    }

    public SourcePosition getTokenPosition() {
        return this.position;
    }

    public String getTokenText() {
        return this.text;
    }

    public TokenType getTokenType() {
        return this.type;
    }

    @Override
    public String toString() {
        if (this.text != null && this.text.length() > 0) {
            return String.format("(%s, \"%s\", %s)", this.type, this.text, this.position);
        } else {
            return String.format("(%s, %s)", this.type, this.position);
        }
    }
}
