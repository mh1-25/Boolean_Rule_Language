package AST;

import AST.Nodes.*;
import AST.Nodes.AssignmentNode;
import AST.Nodes.BinaryExpressionNode;
import AST.Nodes.BooleanLiteralNode;
import AST.Nodes.FunctionCallNode;
import AST.Nodes.IdentifierNode;
import AST.Nodes.NumberLiteralNode;
import AST.Nodes.PrintNode;
import AST.Nodes.PriorityStatementNode;
import AST.Nodes.ProgramNode;
import AST.Nodes.UnaryExpressionNode;

/**
 * ASTPrinter — pretty-prints an AST as an indented tree.
 *
 * NEW: binary expression nodes now show their precedence tier and
 * associativity so you can read the parse tree and understand exactly
 * what grouping the parser chose — the "ambiguity annotation" feature.
 *
 * Example output for:  valid := (score + bonus) >= 60 and attempts < 3;
 *
 *   AssignmentNode [valid :=]
 *   └── BinaryExpr [and]  {prec=1, left-assoc}
 *       ├── BinaryExpr [>=]  {prec=3, left-assoc}
 *       │   ├── BinaryExpr [+]  {prec=4, left-assoc}
 *       │   │   ├── Ident [score]
 *       │   │   └── Ident [bonus]
 *       │   └── Num [60]
 *       └── BinaryExpr [<]  {prec=3, left-assoc}
 *           ├── Ident [attempts]
 *           └── Num [3]
 */
public class ASTPrinter implements ASTVisitor<Void> {

    // Tree-drawing state
    private String  prefix          = "";
    private boolean isLast          = true;
    private boolean isRoot          = true;

    // Set by a parent node before calling a child's accept(); consumed by printHeader.
    private String  pendingAnnotation = "";

    // ─────────────────────────────────────────────
    // Operator metadata (for ambiguity annotation)
    // ─────────────────────────────────────────────

    /**
     * Returns the precedence level of a binary operator.
     * Higher number = tighter binding.
     *
     *   1  — or
     *   2  — and
     *   3  — = != < > <= >=   (comparison)
     *   4  — + -              (addition)
     *   5  — * /              (multiplication)
     */
    private static int precedenceOf(String op) {
        return switch (op) {
            case "or"                        -> 1;
            case "and"                       -> 2;
            case "=", "!=", "<", ">",
                 "<=", ">="                  -> 3;
            case "+", "-"                    -> 4;
            case "*", "/"                    -> 5;
            default                          -> 0;
        };
    }

    /**
     * Returns "right-assoc" for operators that bind right-to-left,
     * "left-assoc" for all others. In this language only unary "not"
     * and unary "-" are right-associative; all binary operators are
     * left-associative.
     */
    private static String assocOf(String op) {
        return "left-assoc";
    }

    // ─────────────────────────────────────────────
    // Public static convenience methods
    // ─────────────────────────────────────────────

    public static void print(AST node) {
        node.accept(new ASTPrinter());
        System.out.println();
    }

    public static void printProgram(ProgramNode program) {
        System.out.println("=== AST ===");
        program.accept(new ASTPrinter());
        System.out.println();
    }

    // ─────────────────────────────────────────────
    // ASTVisitor implementation
    // ─────────────────────────────────────────────

    @Override
    public Void visitProgram(ProgramNode n) {
        printHeader("Program [" + n.statements.size() + " statement(s)]");
        printChildren(n.statements.toArray(AST[]::new));
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentNode n) {
        printHeader("AssignmentNode [" + n.identifier + " :=]");
        printChildren(n.value);
        return null;
    }

    @Override
    public Void visitPriority(PriorityStatementNode n) {
        printHeader("PriorityStmt [priority=" + n.priority + "]");
        printChildren(n.statement);
        return null;
    }

    @Override
    public Void visitPrint(PrintNode n) {
        printHeader("PrintNode");
        printChildren(n.expression);
        return null;
    }

    /**
     * FIX + NEW: Shows precedence tier and associativity on every binary node.
     * This makes it trivial to verify that the parser respected operator precedence.
     *
     * Format: "BinaryExpr [<op>]  {prec=N, left-assoc}"
     *
     * Additionally, if a child has LOWER precedence than the parent
     * (which shouldn't happen in a correct parse tree but is useful to flag),
     * a warning marker is appended.
     */
    @Override
    public Void visitBinaryExpression(BinaryExpressionNode n) {
        int    myPrec = precedenceOf(n.operator);
        String ann    = "  {prec=" + myPrec + ", " + assocOf(n.operator) + "}";
        printHeader("BinaryExpr [" + n.operator + "]" + ann);
        printBinaryChildren(n, myPrec);
        return null;
    }

    /**
     * Unary nodes show their type: "not" is logical (right-assoc),
     * "-" is arithmetic negation (right-assoc).
     */
    @Override
    public Void visitUnaryExpression(UnaryExpressionNode n) {
        String kind = n.operator.equals("not") ? "logical-not" : "arith-neg";
        printHeader("UnaryExpr [" + n.operator + "]  {" + kind + ", right-assoc}");
        printChildren(n.operand);
        return null;
    }

    @Override
    public Void visitFunctionCall(FunctionCallNode n) {
        String kind = switch (n.name.toLowerCase()) {
            case "any", "all"            -> "logical-agg";
            case "sum", "min", "max",
                 "count", "avg"          -> "numeric-agg";
            default                      -> "user-func";
        };
        printHeader("FuncCall [" + n.name + "]  {" + n.arguments.size() + " arg(s), " + kind + "}");
        printChildren(n.arguments.toArray(AST[]::new));
        return null;
    }

    @Override
    public Void visitNumber(NumberLiteralNode n) {
        if (n.value == Math.floor(n.value) && !Double.isInfinite(n.value))
            printHeader("Num [" + (long) n.value + "]");
        else
            printHeader("Num [" + n.value + "]");
        return null;
    }

    @Override
    public Void visitBoolean(BooleanLiteralNode n) {
        printHeader("Bool [" + n.value + "]");
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierNode n) {
        printHeader("Ident [" + n.name + "]");
        return null;
    }

    

    // ─────────────────────────────────────────────
    // NEW: Parenthesized expression view
    // ─────────────────────────────────────────────

    /**
     * Renders a node as a fully-parenthesized infix string.
     * This makes precedence grouping 100% explicit — no ambiguity.
     *
     * Example:  a + b * c   →   (a + (b * c))
     *           a and b or c → ((a and b) or c)
     *
     * Usage:  ASTPrinter.toParenString(expressionNode)
     */
    public static String toParenString(AST node) {
        return switch (node.getType()) {
            case binaryExpression -> {
                BinaryExpressionNode b = (BinaryExpressionNode) node;
                yield "(" + toParenString(b.left) + " " + b.operator + " " + toParenString(b.right) + ")";
            }
            case unaryExpression -> {
                UnaryExpressionNode u = (UnaryExpressionNode) node;
                yield "(" + u.operator + " " + toParenString(u.operand) + ")";
            }
            case functionCall -> {
                FunctionCallNode f = (FunctionCallNode) node;
                StringBuilder sb = new StringBuilder(f.name).append("(");
                for (int i = 0; i < f.arguments.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(toParenString(f.arguments.get(i)));
                }
                sb.append(")");
                yield sb.toString();
            }
            case NumericalLiteral -> {
                NumberLiteralNode num = (NumberLiteralNode) node;
                if (num.value == Math.floor(num.value) && !Double.isInfinite(num.value))
                    yield String.valueOf((long) num.value);
                yield String.valueOf(num.value);
            }
            case BooleanLiteral -> String.valueOf(((BooleanLiteralNode) node).value);
            case identifier     -> ((IdentifierNode) node).name;
            default             -> node.toString();
        };
    }

    /**
     * Prints the fully-parenthesized form of every statement in a program.
     * Useful for verifying that precedence was resolved correctly.
     *
     * Example output:
     *   [valid :=] ((score + bonus) >= 60)
     *              → expression: (((score + bonus) >= 60) and (attempts < 3))
     */
    public static void printParenProgram(Nodes.ProgramNode program) {
        System.out.println("=== Parenthesized Expressions (precedence check) ===");
        for (AST stmt : program.statements) {
            printParenStatement(stmt, "  ");
        }
        System.out.println();
    }

    private static void printParenStatement(AST stmt, String indent) {
        switch (stmt.getType()) {
            case assignmentStatement -> {
                Nodes.AssignmentNode a = (Nodes.AssignmentNode) stmt;
                System.out.println(indent + "[" + a.identifier + " :=]  " + toParenString(a.value));
            }
            case printStatement -> {
                Nodes.PrintNode p = (Nodes.PrintNode) stmt;
                System.out.println(indent + "[print]  " + toParenString(p.expression));
            }
            case priorityStatement -> {
                Nodes.PriorityStatementNode ps = (Nodes.PriorityStatementNode) stmt;
                System.out.print(indent + "[priority " + ps.priority + "] ");
                printParenStatement(ps.statement, ""); // inner stmt on same line (no extra indent)
            }
            default -> System.out.println(indent + stmt);
        }
    }

    // ─────────────────────────────────────────────
    // Tree-drawing helpers (fixed state management)
    // ─────────────────────────────────────────────

    private void printHeader(String label) {
        String ann = pendingAnnotation.isEmpty() ? "" : "  " + pendingAnnotation;
        pendingAnnotation = ""; // always consume

        if (isRoot) {
            System.out.println(label + ann);
        } else {
            System.out.println(prefix + (isLast ? "└── " : "├── ") + label + ann);
        }
    }

    private void printChildren(AST... children) {
        String  savedPrefix = prefix;
        boolean savedIsLast = isLast;
        boolean savedIsRoot = isRoot;

        // Determine the indent to add for children of the current node
        String childIndent  = isRoot ? "" : (isLast ? "    " : "│   ");
        String childPrefix  = prefix + childIndent;

        for (int i = 0; i < children.length; i++) {
            prefix = childPrefix;
            isLast = (i == children.length - 1);
            isRoot = false;
            children[i].accept(this);
        }

        prefix = savedPrefix;
        isLast = savedIsLast;
        isRoot = savedIsRoot;
    }
/**
     * Specialized child printer for binary expression nodes.
     * Sets {@code pendingAnnotation} on each child to explain how the parser
     * resolved precedence at that edge — this is the "ambiguity annotation" feature.
     */
    private void printBinaryChildren(BinaryExpressionNode n, int parentPrec) {
        String  savedPrefix = prefix;
        boolean savedIsLast = isLast;
        boolean savedIsRoot = isRoot;

        String childIndent = isRoot ? "" : (isLast ? "    " : "│   ");
        String childPrefix = prefix + childIndent;

        AST[] children = { n.left, n.right };

        for (int i = 0; i < 2; i++) {
            prefix = childPrefix;
            isLast = (i == 1);
            isRoot = false;

            AST child = children[i];
            pendingAnnotation = buildAmbiguityAnnotation(child, parentPrec, i == 0);
            child.accept(this);
        }

        prefix = savedPrefix;
        isLast = savedIsLast;
        isRoot = savedIsRoot;
    }

    /**
     * Builds the inline ambiguity annotation string for a child of a binary node.
     *
     * @param child      the child AST node
     * @param parentPrec the parent binary node's precedence
     * @param isLeft     true if this is the left child, false if right
     * @return annotation string, or "" if no annotation needed
     */
    private static String buildAmbiguityAnnotation(AST child, int parentPrec, boolean isLeft) {
        if (child.getType() != AST_Type.binaryExpression) return "";

        BinaryExpressionNode childBin  = (BinaryExpressionNode) child;
        int                  childPrec = precedenceOf(childBin.operator);

        if (childPrec > parentPrec) {
            return "<- tighter binding (prec " + childPrec + " > " + parentPrec + ")";
        }
        if (childPrec == parentPrec) {
            if (isLeft) {
                return "<- left-assoc: (a " + childBin.operator + " b) then op c";
            } else {
                return "<- user-grouped: a op (b " + childBin.operator + " c)";
            }
        }
        // childPrec < parentPrec — should never happen in a correct parse tree
        return "<- !! PARSER BUG: child prec " + childPrec + " < parent prec " + parentPrec;
    }
}