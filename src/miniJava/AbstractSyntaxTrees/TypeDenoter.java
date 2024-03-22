/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }
    
    public TypeKind typeKind;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeDenoter other = (TypeDenoter) obj;
        if ((typeKind == TypeKind.VOID && other.typeKind == TypeKind.CLASS) || (typeKind == TypeKind.CLASS && other.typeKind == TypeKind.VOID)) {
            return true;
        }
        if (typeKind != other.typeKind) {
            return false;
        }
        return true;
    }
    
}

        
