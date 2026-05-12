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

public class Interpreter implements ASTVisitor<Object> {

    private final Map<String, Object> environment = new HashMap<>();

    public void run(ProgramNode program) {
        program.accept(this);
    }

    @Override
    public Object visitProgram(ProgramNode n) {
        List<AST> sorted = new ArrayList<>(n.statements);
        sorted.sort(Comparator.comparingInt(stmt -> {
            if (stmt instanceof PriorityStatementNode p) return p.priority;
            return Integer.MAX_VALUE;
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

    @Override
    public Object visitPriority(PriorityStatementNode n) {
        return n.statement.accept(this);
    }

    @Override
    public Object visitBinaryExpression(BinaryExpressionNode n) {
        if (n.operator.equals("or")) {
            if (asBoolean(n.left.accept(this), "or")) return true;
            return asBoolean(n.right.accept(this), "or");
        }
        if (n.operator.equals("and")) {
            if (!asBoolean(n.left.accept(this), "and")) return false;
            return asBoolean(n.right.accept(this), "and");
        }

        Object left  = n.left.accept(this);
        Object right = n.right.accept(this);

        return switch (n.operator) {
            case "+" -> asDouble(left, "+") + asDouble(right, "+");
            case "-" -> asDouble(left, "-") - asDouble(right, "-");
            case "*" -> asDouble(left, "*") * asDouble(right, "*");
            case "/" -> {
                double d = asDouble(right, "/");
                if (d == 0) throw new RuntimeException("Runtime error: division by zero");
                yield asDouble(left, "/") / d;
            }
            case "<"  -> asDouble(left, "<")  <  asDouble(right, "<");
            case ">"  -> asDouble(left, ">")  >  asDouble(right, ">");
            case "<=" -> asDouble(left, "<=") <= asDouble(right, "<=");
            case ">=" -> asDouble(left, ">=") >= asDouble(right, ">=");
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

    @Override
    public Object visitFunctionCall(FunctionCallNode n) {

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

    private String formatValue(Object value) {
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d))
                return String.valueOf(d.longValue());
            return String.valueOf(d);
        }
        return String.valueOf(value);
    }

    public Map<String, Object> getEnvironment() {
        return environment;
    }
}