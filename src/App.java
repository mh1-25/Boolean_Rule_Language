import AST.ASTPrinter;
import AST.Interpreter;
import AST.Nodes;
import AST.Parser;
import Token.Lexer;
import Token.Token;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║       Boolean Rule Language — Interactive Mode       ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Supported tokens:                                   ║");
        System.out.println("║  • Identifiers : any letter/digit word               ║");
        System.out.println("║  • Numbers     : 0 1 42 100 ...                      ║");
        System.out.println("║  • Booleans    : true  false                         ║");
        System.out.println("║  • Logical     : and  or  not                        ║");
        System.out.println("║  • Arithmetic  : +  -  *  /                          ║");
        System.out.println("║  • Comparison  : =  !=  <  >  <=  >=                 ║");
        System.out.println("║  • Assignment  : :=                                  ║");
        System.out.println("║  • Statement   : print  ;                            ║");
        System.out.println("║  • Grouping    : ( )                                 ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Supported statement types (AST nodes):              ║");
        System.out.println("║  • assignmentStatement : x := <expr> ;               ║");
        System.out.println("║  • printStatement      : print <expr> ;              ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Type 'exit' to quit, 'demo' to run built-in tests   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        StringBuilder program = new StringBuilder();

        while (true) {
            System.out.print(">> ");
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (line.equalsIgnoreCase("demo")) {
                runDemoTests();
                continue;
            }

            if (line.equalsIgnoreCase("run")) {
                // Run everything accumulated so far
                if (program.length() == 0) {
                    System.out.println("Nothing to run. Enter some statements first.");
                } else {
                    runProgram("User Program", program.toString());
                    program.setLength(0); // clear after run
                }
                continue;
            }

            if (line.equalsIgnoreCase("clear")) {
                program.setLength(0);
                System.out.println("Program cleared.");
                continue;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (line.isEmpty()) continue;

            // Accumulate lines — auto-run if line ends with ';'
            program.append(line).append("\n");

            // If the line ends with a semicolon, run immediately
            if (line.endsWith(";")) {
                runProgram("Statement", program.toString());
                program.setLength(0);
            }
        }

        scanner.close();
    }

    // ─────────────────────────────────────────────
    // Core runner
    // ─────────────────────────────────────────────

private static void runProgram(String label, String source) {
    System.out.println("┌─────────────────────────────────────────────");
    System.out.println("│ " + label);
    System.out.println("│ Source: " + source.strip().replace("\n", "  "));
    System.out.println("└─────────────────────────────────────────────");

    try {
        // 1. Lex
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        System.out.println("--- Tokens ---");
        for (Token token : tokens) {
            System.out.println("  " + token);
        }

        // 2. Parse → AST
        Parser parser = new Parser(tokens);
        Nodes.ProgramNode ast = parser.parse();

        // 3. Print the AST tree (with precedence/ambiguity annotations)
        ASTPrinter.printProgram(ast);

        // 4. NEW: Print fully-parenthesized form for precedence verification
        ASTPrinter.printParenProgram(ast);

        // 5. Interpret
        System.out.println("--- Output ---");
        Interpreter interpreter = new Interpreter();
        interpreter.run(ast);

    } catch (RuntimeException e) {
        System.out.println("ERROR: " + e.getMessage());
    }

    System.out.println();
}

    // ─────────────────────────────────────────────
    // Help text
    // ─────────────────────────────────────────────

    private static void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    run   — execute the accumulated program");
        System.out.println("    clear — clear the accumulated program");
        System.out.println("    demo  — run all built-in test cases");
        System.out.println("    help  — show this help message");
        System.out.println("    exit  — quit the interpreter");
        System.out.println();
        System.out.println("  Examples:");
        System.out.println("    >> x := 5;");
        System.out.println("    >> y := 10;");
        System.out.println("    >> print x + y;");
        System.out.println();
        System.out.println("  Multi-line (type 'run' when done):");
        System.out.println("    >> score := 55");
        System.out.println("    >> bonus := 10");
        System.out.println("    >> valid := (score + bonus) >= 60 and score < 100;");
        System.out.println("    >> print valid;");
        System.out.println("    >> run");
        System.out.println();
    }

    // ─────────────────────────────────────────────
    // Built-in demo test cases
    // ─────────────────────────────────────────────

    private static void runDemoTests() {
        System.out.println("\n══════════ DEMO TESTS ══════════\n");

        runProgram("Basic arithmetic print",
                "x := 5;\n" +
                "y := 10;\n" +
                "print x + y;\n");

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

        System.out.println("══════════ END DEMO ══════════\n");
    }
}