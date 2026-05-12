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
 * ASTVisitor&lt;R&gt; — generic Visitor interface for the AST node hierarchy.
 *
 * <p>Implement this interface to add a new algorithm (interpretation,
 * pretty-printing, type-checking, code generation, …) over the AST
 * without modifying any node class.
 *
 * <p>Usage pattern — implement for a void walk that prints every node:
 * <pre>
 *   class MyPrinter implements ASTVisitor&lt;Void&gt; {
 *       public Void visitProgram(ProgramNode n) {
 *           n.statements.forEach(s -> s.accept(this));
 *           return null;
 *       }
 *       public Void visitAssignment(AssignmentNode n) {
 *           System.out.println("assign " + n.identifier);
 *           n.value.accept(this);
 *           return null;
 *       }
 *       // … implement the remaining six methods …
 *   }
 * </pre>
 *
 * <p>Usage pattern — implement for a typed evaluation that returns Object:
 * <pre>
 *   class MyInterpreter implements ASTVisitor&lt;Object&gt; {
 *       public Object visitNumber(NumberLiteralNode n) { return n.value; }
 *       public Object visitBool(BooleanLiteralNode n)  { return n.value; }
 *       // …
 *   }
 * </pre>
 *
 * @param <R> the return type of every visit method.
 *            Use {@code Void} (capital V) for side-effect-only visitors
 *            and return {@code null} from each method.
 */
public interface ASTVisitor<R> {

    // ─────────────────────────────────────────────
    // Statements
    // ─────────────────────────────────────────────

    /**
     * Visit the root {@link ProgramNode}.
     * The program holds an ordered list of top-level statements.
     */
    R visitProgram(ProgramNode node);

    /**
     * Visit an {@link AssignmentNode} — {@code identifier := expression ;}.
     */
    R visitAssignment(AssignmentNode node);

    /**
     * Visit a {@link PrintNode} — {@code print expression ;}.
     */
    R visitPrint(PrintNode node);

    // ─────────────────────────────────────────────
    // Expressions — composite
    // ─────────────────────────────────────────────

    /**
     * Visit a {@link BinaryExpressionNode}.
     * The {@code operator} field is the raw token string:
     * arithmetic ({@code + - * /}),
     * comparison ({@code = != < > <= >=}),
     * or logical ({@code and or}).
     */
    R visitBinaryExpression(BinaryExpressionNode node);

    /**
     * Visit a {@link UnaryExpressionNode}.
     * The {@code operator} field is either {@code "not"} (logical negation)
     * or {@code "-"} (arithmetic negation).
     */
    R visitUnaryExpression(UnaryExpressionNode node);

    // ─────────────────────────────────────────────
    // Expressions — leaves
    // ─────────────────────────────────────────────

    /**
     * Visit a {@link NumberLiteralNode} — a numeric constant such as {@code 42}.
     */
    R visitNumber(NumberLiteralNode node);

    /**
     * Visit a {@link BooleanLiteralNode} — {@code true} or {@code false}.
     */
    R visitBoolean(BooleanLiteralNode node);

    /**
     * Visit an {@link IdentifierNode} — a variable reference such as {@code age}.
     */
    R visitIdentifier(IdentifierNode node);
    
    // ─────────────────────────────────────────────
    // Aggregation & priorities
    // ─────────────────────────────────────────────

    /**
     * Visit a {@link FunctionCallNode} — {@code name(arg1, arg2, ...)}.
     * Built-in functions: sum, min, max, count, avg (numeric); any, all (logical).
     */
    R visitFunctionCall(FunctionCallNode node);

    /**
     * Visit a {@link PriorityStatementNode} — {@code priority N statement}.
     * The interpreter executes lower-numbered priorities first.
     */
    R visitPriority(PriorityStatementNode node);
}