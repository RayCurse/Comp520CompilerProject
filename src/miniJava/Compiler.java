package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

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

            Token currentToken = scanner.scan();
            System.out.println(currentToken);

            while (currentToken.type != TokenType.EOT) {
                currentToken = scanner.scan();
                System.out.println(currentToken);
            }
        } finally {
            if (fStream != null) { fStream.close(); }
        }
    }
}
