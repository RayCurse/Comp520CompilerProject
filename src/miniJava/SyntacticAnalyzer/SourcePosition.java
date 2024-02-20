package miniJava.SyntacticAnalyzer;

public class SourcePosition {

    private int lineNumber;
    private int columnNumber;

    public SourcePosition(int lineNumber, int columnNumber) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

	@Override
	public String toString() {
		return String.format("(%d, %d)", this.lineNumber, this.columnNumber);
	}
}
