import java.util.List;

import Token.Lexer;
import Token.Token;
import AST.Parser;
import AST.Nodes;
import AST.ASTPrinter;
import AST.Interpreter;

public class App {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  Boolean Rule Language — Demo        ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // ── Test cases from the project spec ──────────────────────────────
        runProgram("Basic arithmetic print",
                "x := 5;\n" +
                        "y := 10;\n" +
                        "print(x + y);\n");

        runProgram("Adult check",
                "age := 20;\n" +
                        "adult := age >= 18;\n" +
                        "print adult;\n");

        runProgram("Approval rule",
                "income  := 6000;\n" +
                        "blocked := false;\n" +
                        "approved := income > 5000 and not blocked;\n" +
                        "print approved;\n");

        runProgram("Composite rule with arithmetic subexpr",
                "score    := 55;\n" +
                        "bonus    := 10;\n" +
                        "attempts := 2;\n" +
                        "valid := (score + bonus) >= 60 and attempts < 3;\n" +
                        "print valid;\n");

        runProgram("Boolean literals and 'or'",
                "a := true;\n" +
                        "b := false;\n" +
                        "result := a or b;\n" +
                        "print result;\n");

        runProgram("Chained comparisons",
                "x := 7;\n" +
                        "big   := x > 10;\n" +
                        "small := x < 5;\n" +
                        "neither := not big and not small;\n" +
                        "print neither;\n");

        runProgram("Invalid: missing semicolon (syntax error)",
                "x := 42\n" +
                        "print x;\n");

        runProgram("Invalid: undefined variable (runtime error)",
                "print undefinedVar;\n");

        runProgram("Invalid: type mismatch (runtime error)",
                "x := 5 + true;\n" +
                        "print x;\n");
    }

    /** Run a single test case: lex → parse → print AST → interpret */
    private static void runProgram(String label, String source) {
        System.out.println("┌─────────────────────────────────────");
        System.out.println("│ " + label);
        System.out.println("│ Source: " + source.strip().replace("\n", "  "));
        System.out.println("└─────────────────────────────────────");

        try {
            // 1. Lex
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            // 2. Parse → AST
            Parser parser = new Parser(tokens);
            Nodes.ProgramNode ast = parser.parse();

            // 3. Print the AST
            ASTPrinter.printProgram(ast);

            // 4. Interpret
            System.out.println("--- Output ---");
            Interpreter interpreter = new Interpreter();
            interpreter.run(ast);

        } catch (RuntimeException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        System.out.println();
    }
}