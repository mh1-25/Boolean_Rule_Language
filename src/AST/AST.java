package AST;

/**
 * AST — the base interface for every node in the Abstract Syntax Tree.
 *
 * Every concrete node class in {@link Nodes} implements this interface.
 * The two contracts every node must fulfil are:
 *
 *   1. {@link #getType()}  — returns the {@link AST_Type} enum constant that
 *                            identifies what kind of node this is.  Used by the
 *                            interpreter and printer for fast dispatch.
 *
 *   2. {@link #accept(ASTVisitor)} — the Visitor hook.  Concrete nodes call
 *                            the matching {@code visit*} method on the supplied
 *                            visitor so that external algorithms (pretty-print,
 *                            type-check, interpret, …) can be added without
 *                            touching the node classes themselves.
 *
 * Node hierarchy
 * ──────────────
 *  AST
 *  ├── Nodes.ProgramNode           (program)
 *  ├── Nodes.AssignmentNode        (assignmentStatement)
 *  ├── Nodes.PrintNode             (printStatement)
 *  ├── Nodes.BinaryExpressionNode  (binaryExpression)
 *  ├── Nodes.UnaryExpressionNode   (unaryExpression)
 *  ├── Nodes.NumberLiteralNode     (NumericalLiteral)
 *  ├── Nodes.BooleanLiteralNode    (BooleanLiteral)
 *  └── Nodes.IdentifierNode        (identifier)
 */
public interface AST {

    // ─────────────────────────────────────────────
    // Core contract
    // ─────────────────────────────────────────────

    /**
     * Returns the {@link AST_Type} tag for this node.
     * Callers can switch/match on this value for type-based dispatch without
     * needing {@code instanceof} chains.
     *
     * @return the enum constant that names this node's kind
     */
    AST_Type getType();

    /**
     * Accepts a {@link ASTVisitor} and routes to the correct {@code visit*}
     * method.  Every concrete node must implement this as:
     * <pre>
     *     public &lt;R&gt; R accept(ASTVisitor&lt;R&gt; v) { return v.visitXxx(this); }
     * </pre>
     *
     * @param <R>     the return type produced by the visitor
     * @param visitor the visitor to dispatch to
     * @return whatever the visitor method returns
     */
    <R> R accept(ASTVisitor<R> visitor);

    // ─────────────────────────────────────────────
    // Convenience helpers (default implementations)
    // ─────────────────────────────────────────────

    /**
     * Returns {@code true} if this node is a statement
     * ({@code program}, {@code assignmentStatement}, or {@code printStatement}).
     */
    default boolean isStatement() {
        return switch (getType()) {
            case program, assignmentStatement, printStatement -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if this node is an expression
     * (anything that produces a value when evaluated).
     */
    default boolean isExpression() {
        return !isStatement();
    }

    /**
     * Returns a compact, single-line description of this node.
     * Concrete nodes override {@code toString()} to satisfy this contract.
     */
    @Override
    String toString();
}