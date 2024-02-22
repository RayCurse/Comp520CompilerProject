package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = null;
        if (args.length > 0) { filePath = args[0]; }
        if (filePath == null) {
            System.err.println("No path provided");
            System.exit(1);
        }

        FileInputStream fStream = null;
        try {
            fStream = new FileInputStream(filePath);
            Scanner scanner = new Scanner(fStream);
            Parser parser = new Parser(scanner);
            // parser.printTokens = true;
            Package AST = parser.parseTokenStream();
            if (AST != null) {
                // ASTDisplay.showPosition = true;
                ASTDisplay display = new ASTDisplay();
                display.showTree(AST);
            } else {
                System.out.println("Error");
                System.out.println(parser.getErrorMsg());
            }
        } finally {
            if (fStream != null) { fStream.close(); }
        }
    }
}
