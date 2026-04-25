package AST;

import java.util.List;

/**
 * Nodes — all concrete AST node classes in one file.
 *
 * <p>Every static inner class:
 * <ul>
 *   <li>implements {@link AST}</li>
 *   <li>implements {@link AST#getType()} returning the matching {@link AST_Type}</li>
 *   <li>implements {@link AST#accept(ASTVisitor)} for the Visitor pattern</li>
 *   <li>exposes its fields as {@code public final} — no getters needed</li>
 *   <li>overrides {@code toString()} for compact debug output</li>
 * </ul>
 *
 * Grammar (operator precedence, lowest → highest binding)
 * ────────────────────────────────────────────────────────
 * <pre>
 * program    → statement* EOF
 * statement  → assignStmt | printStmt
 * assignStmt → IDENTIFIER ':=' expression ';'
 * printStmt  → 'print' expression ';'
 *
 * expression → orExpr
 * orExpr     → andExpr  ( 'or'  andExpr  )*
 * andExpr    → notExpr  ( 'and' notExpr  )*
 * notExpr    → 'not' notExpr | comparison
 * comparison → addition ( ('='|'!='|'<'|'>'|'<='|'>=') addition )?
 * addition   → multiply  ( ('+'|'-') multiply )*
 * multiply   → unary     ( ('*'|'/') unary    )*
 * unary      → '-' unary | primary
 * primary    → NUMBER | 'true' | 'false' | IDENTIFIER | '(' expression ')'
 * </pre>
 */
public class Nodes {

    // ═════════════════════════════════════════════
    // Root
    // ═════════════════════════════════════════════

    /**
     * ProgramNode — the root of every parse tree.
     *
     * <p>Holds an ordered list of top-level statements produced by the parser.
     * The interpreter runs them in sequence.
     */
    public static class ProgramNode implements AST {

        /** All top-level statements in source order. */
        public final List<AST> statements;

        public ProgramNode(List<AST> statements) {
            this.statements = List.copyOf(statements);   // immutable snapshot
        }

        @Override public AST_Type getType() { return AST_Type.program; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitProgram(this); }

        @Override public String toString() {
            return "ProgramNode{" + statements.size() + " statement(s)}";
        }
    }

    // ═════════════════════════════════════════════
    // Statements
    // ═════════════════════════════════════════════

    /**
     * AssignmentNode — {@code identifier := expression ;}
     *
     * <p>Binds the result of {@code value} to {@code identifier} in the
     * current environment.  Re-assigning an existing variable replaces its value.
     */
    public static class AssignmentNode implements AST {

        /** The name of the variable being assigned, e.g. {@code "age"}. */
        public final String identifier;

        /** The expression whose evaluated result is stored. */
        public final AST value;

        public AssignmentNode(String identifier, AST value) {
            this.identifier = identifier;
            this.value      = value;
        }

        @Override public AST_Type getType() { return AST_Type.assignmentStatement; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitAssignment(this); }

        @Override public String toString() {
            return "AssignmentNode{" + identifier + " := " + value + "}";
        }
    }

    /**
     * PrintNode — {@code print expression ;}
     *
     * <p>Evaluates {@code expression} and writes its value to standard output.
     */
    public static class PrintNode implements AST {

        /** The expression to evaluate and print. */
        public final AST expression;

        public PrintNode(AST expression) {
            this.expression = expression;
        }

        @Override public AST_Type getType() { return AST_Type.printStatement; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitPrint(this); }

        @Override public String toString() {
            return "PrintNode{" + expression + "}";
        }
    }

    // ═════════════════════════════════════════════
    // Composite expressions
    // ═════════════════════════════════════════════

    /**
     * BinaryExpressionNode — {@code left operator right}
     *
     * <p>Covers all infix operators in the language:
     * <ul>
     *   <li>Arithmetic:  {@code +  -  *  /}</li>
     *   <li>Comparison:  {@code =  !=  <  >  <=  >=}</li>
     *   <li>Logical:     {@code and  or}</li>
     * </ul>
     *
     * The {@code operator} field stores the raw token string exactly as it
     * appears in the source, so the interpreter can switch on it directly.
     */
    public static class BinaryExpressionNode implements AST {

        /** Left-hand operand. */
        public final AST left;

        /**
         * Operator token string, e.g. {@code "+"}, {@code ">="}, {@code "and"}.
         */
        public final String operator;

        /** Right-hand operand. */
        public final AST right;

        public BinaryExpressionNode(AST left, String operator, AST right) {
            this.left     = left;
            this.operator = operator;
            this.right    = right;
        }

        @Override public AST_Type getType() { return AST_Type.binaryExpression; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitBinaryExpression(this); }

        @Override public String toString() {
            return "BinaryExpr{" + left + " " + operator + " " + right + "}";
        }
    }

    /**
     * UnaryExpressionNode — {@code operator operand}
     *
     * <p>Two unary operators exist in this language:
     * <ul>
     *   <li>{@code "not"} — logical negation; operand must be boolean</li>
     *   <li>{@code "-"}   — arithmetic negation; operand must be numeric</li>
     * </ul>
     */
    public static class UnaryExpressionNode implements AST {

        /**
         * Operator token string: {@code "not"} or {@code "-"}.
         */
        public final String operator;

        /** The single operand this operator applies to. */
        public final AST operand;

        public UnaryExpressionNode(String operator, AST operand) {
            this.operator = operator;
            this.operand  = operand;
        }

        @Override public AST_Type getType() { return AST_Type.unaryExpression; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitUnaryExpression(this); }

        @Override public String toString() {
            return "UnaryExpr{" + operator + " " + operand + "}";
        }
    }

    // ═════════════════════════════════════════════
    // Leaf expressions
    // ═════════════════════════════════════════════

    /**
     * NumberLiteralNode — a numeric constant, e.g. {@code 42} or {@code 3.14}.
     *
     * <p>Stored internally as {@code double} to support both integer and
     * floating-point values.  The interpreter formats whole numbers without
     * a decimal point.
     */
    public static class NumberLiteralNode implements AST {

        /** The numeric value as parsed from the source. */
        public final double value;

        public NumberLiteralNode(double value) {
            this.value = value;
        }

        @Override public AST_Type getType() { return AST_Type.NumericalLiteral; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitNumber(this); }

        @Override public String toString() {
            if (value == Math.floor(value) && !Double.isInfinite(value))
                return "Num{" + (long) value + "}";
            return "Num{" + value + "}";
        }
    }

    /**
     * BooleanLiteralNode — the keyword {@code true} or {@code false}.
     */
    public static class BooleanLiteralNode implements AST {

        /** {@code true} or {@code false}. */
        public final boolean value;

        public BooleanLiteralNode(boolean value) {
            this.value = value;
        }

        @Override public AST_Type getType() { return AST_Type.BooleanLiteral; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitBoolean(this); }

        @Override public String toString() {
            return "Bool{" + value + "}";
        }
    }

    /**
     * IdentifierNode — a variable reference, e.g. {@code age} or {@code approved}.
     *
     * <p>At runtime the interpreter looks up {@code name} in the environment map
     * and returns the stored value, or throws if the variable is undefined.
     */
    public static class IdentifierNode implements AST {

        /** The variable name exactly as written in the source. */
        public final String name;

        public IdentifierNode(String name) {
            this.name = name;
        }

        @Override public AST_Type getType() { return AST_Type.identifier; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitIdentifier(this); }

        @Override public String toString() {
            return "Ident{" + name + "}";
        }
    }
}