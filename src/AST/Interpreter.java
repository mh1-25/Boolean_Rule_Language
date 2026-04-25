package AST;

import java.util.HashMap;
import java.util.Map;

import AST.Nodes.*;

/**
 * Interpreter — tree-walking evaluator that implements {@link ASTVisitor}&lt;Object&gt;.
 *
 * <p>Values in this language are either:
 * <ul>
 *   <li>{@code Double}  — for numeric results</li>
 *   <li>{@code Boolean} — for boolean results</li>
 * </ul>
 *
 * Any type mismatch raises a {@link RuntimeException} with a descriptive message.
 *
 * <p>Usage:
 * <pre>
 *   Interpreter interp = new Interpreter();
 *   interp.run(programNode);
 * </pre>
 */
public class Interpreter implements ASTVisitor<Object> {

    /** Variable store: maps identifier names → current runtime values. */
    private final Map<String, Object> environment = new HashMap<>();

    // ─────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────

    /** Execute a full program node. */
    public void run(ProgramNode program) {
        program.accept(this);
    }

    // ─────────────────────────────────────────────
    // ASTVisitor — statements
    // ─────────────────────────────────────────────

    @Override
    public Object visitProgram(ProgramNode n) {
        for (var stmt : n.statements) stmt.accept(this);
        return null;
    }

    @Override
    public Object visitAssignment(AssignmentNode n) {
        Object value = n.value.accept(this);
        environment.put(n.identifier, value);
        return null;
    }

    @Override
    public Object visitPrint(PrintNode n) {
        System.out.println(formatValue(n.expression.accept(this)));
        return null;
    }

    // ─────────────────────────────────────────────
    // ASTVisitor — composite expressions
    // ─────────────────────────────────────────────

    @Override
    public Object visitBinaryExpression(BinaryExpressionNode n) {
        // Short-circuit logical operators — right side evaluated only when needed
        if (n.operator.equals("or")) {
            if (asBoolean(n.left.accept(this), "or")) return true;
            return asBoolean(n.right.accept(this), "or");
        }
        if (n.operator.equals("and")) {
            if (!asBoolean(n.left.accept(this), "and")) return false;
            return asBoolean(n.right.accept(this), "and");
        }

        // All other operators: evaluate both sides eagerly
        Object left  = n.left.accept(this);
        Object right = n.right.accept(this);

        return switch (n.operator) {
            // Arithmetic
            case "+" -> asDouble(left, "+") + asDouble(right, "+");
            case "-" -> asDouble(left, "-") - asDouble(right, "-");
            case "*" -> asDouble(left, "*") * asDouble(right, "*");
            case "/" -> {
                double d = asDouble(right, "/");
                if (d == 0) throw new RuntimeException("Runtime error: division by zero");
                yield asDouble(left, "/") / d;
            }
            // Comparison (numeric only)
            case "<"  -> asDouble(left, "<")  <  asDouble(right, "<");
            case ">"  -> asDouble(left, ">")  >  asDouble(right, ">");
            case "<=" -> asDouble(left, "<=") <= asDouble(right, "<=");
            case ">=" -> asDouble(left, ">=") >= asDouble(right, ">=");
            // Equality (works for both types; mixed types are never equal)
            case "="  -> valuesEqual(left, right);
            case "!=" -> !valuesEqual(left, right);
            default   -> throw new RuntimeException(
                "Runtime error: unknown operator '" + n.operator + "'"
            );
        };
    }

    @Override
    public Object visitUnaryExpression(UnaryExpressionNode n) {
        Object operand = n.operand.accept(this);
        return switch (n.operator) {
            case "not" -> !asBoolean(operand, "not");
            case "-"   -> -asDouble(operand,  "unary -");
            default    -> throw new RuntimeException(
                "Runtime error: unknown unary operator '" + n.operator + "'"
            );
        };
    }

    // ─────────────────────────────────────────────
    // ASTVisitor — leaf expressions
    // ─────────────────────────────────────────────

    @Override
    public Object visitNumber(NumberLiteralNode n) { return n.value; }

    @Override
    public Object visitBoolean(BooleanLiteralNode n) { return n.value; }

    @Override
    public Object visitIdentifier(IdentifierNode n) {
        if (!environment.containsKey(n.name))
            throw new RuntimeException(
                "Runtime error: undefined variable '" + n.name + "'"
            );
        return environment.get(n.name);
    }

    // ─────────────────────────────────────────────
    // Type-coercion helpers with clear error messages
    // ─────────────────────────────────────────────

    private double asDouble(Object value, String ctx) {
        if (value instanceof Double d) return d;
        throw new RuntimeException(
            "Type error: operator '" + ctx + "' requires a number, got "
            + typeName(value) + " (" + value + ")"
        );
    }

    private boolean asBoolean(Object value, String ctx) {
        if (value instanceof Boolean b) return b;
        throw new RuntimeException(
            "Type error: operator '" + ctx + "' requires a boolean, got "
            + typeName(value) + " (" + value + ")"
        );
    }

    private boolean valuesEqual(Object a, Object b) {
        if (a instanceof Double da && b instanceof Double db) return da.equals(db);
        if (a instanceof Boolean ba && b instanceof Boolean bb) return ba.equals(bb);
        return false;
    }

    private String typeName(Object v) {
        if (v instanceof Double)  return "number";
        if (v instanceof Boolean) return "boolean";
        return v == null ? "null" : v.getClass().getSimpleName();
    }

    // ─────────────────────────────────────────────
    // Output formatting
    // ─────────────────────────────────────────────

    private String formatValue(Object value) {
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d))
                return String.valueOf(d.longValue());
            return String.valueOf(d);
        }
        return String.valueOf(value);
    }

    /** Expose the variable store for inspection / testing. */
    public Map<String, Object> getEnvironment() {
        return environment;
    }
}