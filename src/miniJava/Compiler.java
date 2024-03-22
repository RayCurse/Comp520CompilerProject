package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.ContextualAnalysisVisitor;
import miniJava.ContextualAnalysis.Environment;
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
            fStream.close();
            if (AST == null) {
                System.out.println("Error");
                System.out.println(parser.getErrorMsg());
                System.exit(0);
            }

            ContextualAnalysisVisitor contextualAnalysisVisitor = new ContextualAnalysisVisitor();
            Environment env = new Environment();
            AST.visit(contextualAnalysisVisitor, env);

            ASTDisplay display = new ASTDisplay();
            display.showTree(AST);

            if (env.errorMessages.size() > 0) {
                System.out.println("Error");
                for (String errorMessage : env.errorMessages) {
                    System.out.println(errorMessage);
                }
            } else {
                System.out.println("Success");
            }
        } finally {
            if (fStream != null) { fStream.close(); }
        }
    }
}
