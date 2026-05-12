package AST;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreter — tree-walking evaluator that implements {@link ASTVisitor}&lt;Object&gt;.
 *
 * <p>Values in this language are either:
 * <ul>
 *   <li>{@code Double}  — for numeric results</li>
 *   <li>{@code Boolean} — for boolean results</li>
 * </ul>
 *
 * <h3>Priority execution</h3>
 * {@code visitProgram} performs a stable sort of statements by priority before
 * executing them.  Statements wrapped in {@link PriorityStatementNode} carry an
 * integer priority; statements without a priority annotation are assigned
 * {@link Integer#MAX_VALUE} and therefore always run last, in source order.
 *
 * <h3>Aggregation functions</h3>
 * {@code visitFunctionCall} dispatches to built-in functions:
 * <ul>
 *   <li>Numeric: {@code sum  min  max  count  avg}</li>
 *   <li>Logical: {@code any  all}  (short-circuit evaluation)</li>
 * </ul>
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
        List<AST> sorted = new ArrayList<>(n.statements);
        // Stable sort: same-priority statements keep their source order.
        sorted.sort(Comparator.comparingInt(stmt -> {
            if (stmt instanceof PriorityStatementNode p) return p.priority;
            return Integer.MAX_VALUE; // no priority → run last
        }));
        for (AST stmt : sorted) stmt.accept(this);
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

    /** Unwrap and execute the inner statement. */
    @Override
    public Object visitPriority(PriorityStatementNode n) {
        return n.statement.accept(this);
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
    // ASTVisitor — aggregation function calls
    // ─────────────────────────────────────────────

    /**
     * Dispatches to built-in aggregation functions.
     *
     * <p>Logical aggregators ({@code any}, {@code all}) use short-circuit evaluation:
     * arguments are evaluated one by one and the function returns as soon as the
     * result is determined.
     *
     * <p>Numeric aggregators ({@code sum}, {@code min}, {@code max}, {@code count},
     * {@code avg}) evaluate all arguments eagerly.
     *
     * @throws RuntimeException for unknown function names or type mismatches
     */
    @Override
    public Object visitFunctionCall(FunctionCallNode n) {

        // ── Logical aggregation (short-circuit) ──────────────────────────────
        switch (n.name.toLowerCase()) {
            case "any" -> {
                for (AST arg : n.arguments) {
                    if (asBoolean(arg.accept(this), "any")) return true;
                }
                return false;
            }
            case "all" -> {
                for (AST arg : n.arguments) {
                    if (!asBoolean(arg.accept(this), "all")) return false;
                }
                return true;
            }
        }

        // ── Numeric aggregation (eager) ──────────────────────────────────────
        List<Object> vals = new ArrayList<>();
        for (AST arg : n.arguments) vals.add(arg.accept(this));

        return switch (n.name.toLowerCase()) {
            case "sum" -> {
                double s = 0;
                for (Object v : vals) s += asDouble(v, "sum");
                yield s;
            }
            case "min" -> {
                requireArgs(n.name, vals, 1);
                double m = asDouble(vals.get(0), "min");
                for (int i = 1; i < vals.size(); i++)
                    m = Math.min(m, asDouble(vals.get(i), "min"));
                yield m;
            }
            case "max" -> {
                requireArgs(n.name, vals, 1);
                double m = asDouble(vals.get(0), "max");
                for (int i = 1; i < vals.size(); i++)
                    m = Math.max(m, asDouble(vals.get(i), "max"));
                yield m;
            }
            case "count" -> (double) vals.size();
            case "avg" -> {
                requireArgs(n.name, vals, 1);
                double s = 0;
                for (Object v : vals) s += asDouble(v, "avg");
                yield s / vals.size();
            }
            default -> throw new RuntimeException(
                "Runtime error: unknown function '" + n.name +
                "'. Built-in functions: sum, min, max, count, avg, any, all"
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
        if (a instanceof Double  da && b instanceof Double  db) return da.equals(db);
        if (a instanceof Boolean ba && b instanceof Boolean bb) return ba.equals(bb);
        return false;
    }

    private String typeName(Object v) {
        if (v instanceof Double)  return "number";
        if (v instanceof Boolean) return "boolean";
        return v == null ? "null" : v.getClass().getSimpleName();
    }

    private void requireArgs(String name, List<Object> args, int min) {
        if (args.size() < min)
            throw new RuntimeException(
                "Runtime error: " + name + "() requires at least " + min +
                " argument(s), got " + args.size()
            );
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