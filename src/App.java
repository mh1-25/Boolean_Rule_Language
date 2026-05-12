import AST.ASTPrinter;
import AST.Interpreter;
import AST.Nodes;
import AST.Parser;
import Token.Lexer;
import Token.Token;
import java.util.List;
import java.util.Scanner;

public class App {

    private static Interpreter replInterpreter = new Interpreter();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║       Boolean Rule Language — Interactive Mode       ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Supported tokens:                                   ║");
        System.out.println("║  • Identifiers : any letter/digit word               ║");
        System.out.println("║  • Numbers     : 0 1 42 3.14 ...                     ║");
        System.out.println("║  • Booleans    : true  false                         ║");
        System.out.println("║  • Logical     : and  or  not                        ║");
        System.out.println("║  • Arithmetic  : +  -  *  /                          ║");
        System.out.println("║  • Comparison  : =  !=  <  >  <=  >=                 ║");
        System.out.println("║  • Assignment  : :=                                  ║");
        System.out.println("║  • Statement   : print  ;                            ║");
        System.out.println("║  • Grouping    : ( )                                 ║");
        System.out.println("║  • Aggregation : sum() min() max() count() avg()     ║");
        System.out.println("║                  any() all()                         ║");
        System.out.println("║  • Priorities  : priority N statement                ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Statement types:                                    ║");
        System.out.println("║  • x := <expr> ;                                     ║");
        System.out.println("║  • print <expr> ;                                    ║");
        System.out.println("║  • priority N  x := <expr> ;                         ║");
        System.out.println("║                                                      ║");
        System.out.println("║  Commands: exit  demo  run  clear  reset  help       ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        StringBuilder program = new StringBuilder();

        while (true) {
            System.out.print(">> ");

            if (!scanner.hasNextLine()) {
                System.out.println("Goodbye!");
                break;
            }
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
                if (program.length() == 0) {
                    System.out.println("Nothing to run. Enter some statements first.");
                } else {
                    runProgram("User Program", program.toString(), replInterpreter);
                    program.setLength(0);
                }
                continue;
            }

            if (line.equalsIgnoreCase("clear")) {
                program.setLength(0);
                System.out.println("Program cleared.");
                continue;
            }

            if (line.equalsIgnoreCase("reset")) {
                program.setLength(0);
                replInterpreter = new Interpreter();
                System.out.println("Program and variable store cleared.");
                continue;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (line.isEmpty()) continue;

            program.append(line).append("\n");

            if (line.endsWith(";")) {
                runProgram("Statement", program.toString(), replInterpreter);
                program.setLength(0);
            }
        }

        scanner.close();
    }

    private static void runProgram(String label, String source) {
        runProgram(label, source, new Interpreter());
    }

    private static void runProgram(String label, String source, Interpreter interpreter) {
        System.out.println("┌─────────────────────────────────────────────");
        System.out.println("│ " + label);
        System.out.println("│ Source: " + source.strip().replace("\n", "  "));
        System.out.println("└─────────────────────────────────────────────");

        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            System.out.println("--- Tokens ---");
            for (Token token : tokens) {
                System.out.println("  " + token);
            }

            Parser parser = new Parser(tokens);
            Nodes.ProgramNode ast = parser.parse();

            ASTPrinter.printProgram(ast);

            ASTPrinter.printParenProgram(ast);

            System.out.println("--- Output ---");
            interpreter.run(ast);

        } catch (RuntimeException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        System.out.println();
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    run   — execute the accumulated program");
        System.out.println("    clear — clear the accumulated program buffer");
        System.out.println("    reset — clear buffer AND all variables");
        System.out.println("    demo  — run all built-in test cases");
        System.out.println("    help  — show this help message");
        System.out.println("    exit  — quit the interpreter");
        System.out.println();
        System.out.println("  Single-statement (auto-runs on ';'):");
        System.out.println("    >> x := 5;");
        System.out.println("    >> print x + 10;");
        System.out.println();
        System.out.println("  Multi-line (type 'run' when done):");
        System.out.println("    >> score := 55");
        System.out.println("    >> bonus := 10");
        System.out.println("    >> valid := (score + bonus) >= 60 and score < 100;");
        System.out.println("    >> run");
        System.out.println();
        System.out.println("  Aggregation functions:");
        System.out.println("    >> t := sum(10, 20, 30);");
        System.out.println("    >> print min(5, 3, 8);");
        System.out.println("    >> print any(false, true, false);");
        System.out.println("    >> print all(true, true, false);");
        System.out.println();
        System.out.println("  Rule priorities (lower number = runs first):");
        System.out.println("    >> priority 2  y := x * 2;");
        System.out.println("    >> priority 1  x := 10;");
        System.out.println("    >> run   =>  y = 20  (x computed first due to priority 1)");
        System.out.println();
    }

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

        runProgram("Chained comparisons resolved with 'and'",
                "x := 7;\n" +
                "big   := x > 10;\n" +
                "small := x < 5;\n" +
                "neither := not big and not small;\n" +
                "print neither;\n");

        runProgram("Precedence: a + b * c  (child annotation shows tighter binding)",
                "a := 2;\n" +
                "b := 3;\n" +
                "c := 4;\n" +
                "result := a + b * c;\n" + 
                "print result;\n");

        runProgram("Associativity: a - b - c  (left-assoc annotation on left child)",
                "a := 10;\n" +
                "b := 3;\n" +
                "c := 2;\n" +
                "result := a - b - c;\n" + 
                "print result;\n");

        runProgram("Mixed precedence: a or b and c  (and binds tighter than or)",
                "a := false;\n" +
                "b := true;\n" +
                "c := true;\n" +
                "result := a or b and c;\n" + 
                "print result;\n");


        runProgram("Numeric aggregation: sum, min, max, count, avg",
                "total   := sum(10, 20, 30);\n" +
                "lowest  := min(10, 20, 30);\n" +
                "highest := max(10, 20, 30);\n" +
                "n       := count(10, 20, 30);\n" +
                "average := avg(10, 20, 30);\n" +
                "print total;\n" +
                "print lowest;\n" +
                "print highest;\n" +
                "print n;\n" +
                "print average;\n");

        runProgram("Logical aggregation: any, all (short-circuit)",
                "a := true;\n" +
                "b := false;\n" +
                "c := true;\n" +
                "print any(a, b, c);\n" +   
                "print all(a, b, c);\n" +   
                "print any(false, false);\n" + 
                "print all(true, true);\n");    

        runProgram("Aggregation in complex rule",
                "scores := sum(85, 90, 78);\n" +
                "pass   := scores >= 240 and all(85 >= 70, 90 >= 70, 78 >= 70);\n" +
                "print scores;\n" +
                "print pass;\n");


        runProgram("Priority: statements written out of dependency order",
                "priority 2  y := x * 2;\n" +
                "priority 1  x := 10;\n" +        
                "print y;\n");                     

        runProgram("Priority: multiple levels",
                "priority 3  z := x + y;\n" +  
                "priority 1  x := 5;\n" +  
                "priority 2  y := x * 3;\n" +  
                "print z;\n");                     

        runProgram("Priority: non-priority statements run after all priority ones",
                "print msg;\n" +             
                "priority 1  msg := true;\n" +     
                "priority 2  msg := false;\n");  

        runProgram("Error: missing semicolon (syntax error)",
                "x := 42\n" +
                "print x;\n");

        runProgram("Error: undefined variable (runtime error)",
                "print undefinedVar;\n");

        runProgram("Error: type mismatch (runtime error)",
                "x := 5 + true;\n" +
                "print x;\n");

        runProgram("Error: unknown function (runtime error)",
                "print foo(1, 2);\n");

        System.out.println("══════════ END DEMO ══════════\n");
    }
}