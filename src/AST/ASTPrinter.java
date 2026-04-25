package AST;

import AST.Nodes.*;

/**
 * ASTPrinter — pretty-prints an AST as an indented tree by implementing
 * {@link ASTVisitor}&lt;Void&gt;.
 *
 * <p>Sample output for {@code valid := (score + bonus) >= 60 and attempts < 3;}
 *
 * <pre>
 * AssignmentNode [valid :=]
 * └── BinaryExpr [and]
 *     ├── BinaryExpr [>=]
 *     │   ├── BinaryExpr [+]
 *     │   │   ├── Ident [score]
 *     │   │   └── Ident [bonus]
 *     │   └── Num [60]
 *     └── BinaryExpr [<]
 *         ├── Ident [attempts]
 *         └── Num [3]
 * </pre>
 *
 * <p>Usage:
 * <pre>
 *   ASTPrinter printer = new ASTPrinter();
 *   ast.accept(printer);              // print any single node
 *   ASTPrinter.printProgram(program); // convenience for a full program
 * </pre>
 */
public class ASTPrinter implements ASTVisitor<Void> {

    // Current indentation prefix passed down through the recursion
    private String prefix  = "";
    // Whether the node being printed is the last child of its parent
    private boolean isLast = true;
    // Whether this is the very first (root-level) call — roots get no connector
    private boolean isRoot = true;

    // ─────────────────────────────────────────────
    // Public static convenience methods
    // ─────────────────────────────────────────────

    /** Print any single AST node as a tree. */
    public static void print(AST node) {
        node.accept(new ASTPrinter());
        System.out.println();
    }

    /** Print a full program, each statement as its own sub-tree. */
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
    public Void visitPrint(PrintNode n) {
        printHeader("PrintNode");
        printChildren(n.expression);
        return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpressionNode n) {
        printHeader("BinaryExpr [" + n.operator + "]");
        printChildren(n.left, n.right);
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpressionNode n) {
        printHeader("UnaryExpr [" + n.operator + "]");
        printChildren(n.operand);
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
    // Tree-drawing helpers
    // ─────────────────────────────────────────────

    /**
     * Prints the connector + label line for the current node.
     *
     * <pre>
     *   isRoot  → no connector, no indent change
     *   isLast  → "└── "
     *   else    → "├── "
     * </pre>
     */
    private void printHeader(String label) {
        if (isRoot) {
            System.out.println(label);
        } else {
            String connector = isLast ? "└── " : "├── ";
            System.out.println(prefix + connector + label);
        }
    }

    /**
     * Recurse into each child, updating the prefix and isLast flags so the
     * tree-drawing characters come out correctly.
     */
    private void printChildren(AST... children) {
        // Save current state so we can restore after the recursion
        String  savedPrefix = prefix;
        boolean savedIsLast = isLast;
        boolean savedIsRoot = isRoot;

        // Children are indented one level deeper
        String childIndent = isRoot ? "" : (isLast ? "    " : "│   ");
        String childPrefix = prefix + childIndent;

        for (int i = 0; i < children.length; i++) {
            prefix  = childPrefix;
            isLast  = (i == children.length - 1);
            isRoot  = false;
            children[i].accept(this);
        }

        // Restore
        prefix  = savedPrefix;
        isLast  = savedIsLast;
        isRoot  = savedIsRoot;
    }
}