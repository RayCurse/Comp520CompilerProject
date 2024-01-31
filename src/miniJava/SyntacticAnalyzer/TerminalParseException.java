package miniJava.SyntacticAnalyzer;

class TerminalParseException extends Exception {
    public TerminalParseException(String message, Throwable cause) { super(message, cause); }
    public TerminalParseException(String message) { super(message); }
    public TerminalParseException(Throwable cause) { super(cause); }
    public TerminalParseException() { super(); }
}
