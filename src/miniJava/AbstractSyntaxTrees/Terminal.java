/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

abstract public class Terminal extends AST {

  public Terminal (Token t) {
	super(t.getTokenPosition());
    spelling = t.getTokenText();
    kind = t.getTokenType();
  }

  public TokenType kind;
  public String spelling;
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Terminal other = (Terminal) obj;
        if (kind != other.kind)
            return false;
        if (spelling == null) {
            if (other.spelling != null)
                return false;
        } else if (!spelling.equals(other.spelling))
            return false;
        return true;
    }
}
