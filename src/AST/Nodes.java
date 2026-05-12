package AST;

import java.util.List;

public class Nodes {

    public static class ProgramNode implements AST {

        public final List<AST> statements;

        public ProgramNode(List<AST> statements) {
            this.statements = List.copyOf(statements);
        }

        @Override public AST_Type getType() { return AST_Type.program; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitProgram(this); }

        @Override public String toString() {
            return "ProgramNode{" + statements.size() + " statement(s)}";
        }
    }

    public static class AssignmentNode implements AST {

        public final String identifier;
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

    public static class PrintNode implements AST {

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

    public static class BinaryExpressionNode implements AST {

        public final AST left;
        public final String operator;
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

    public static class UnaryExpressionNode implements AST {

        public final String operator;

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

    public static class NumberLiteralNode implements AST {

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


    public static class BooleanLiteralNode implements AST {

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

    public static class IdentifierNode implements AST {

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

    public static class FunctionCallNode implements AST {

        public final String name;

        public final List<AST> arguments;

        public FunctionCallNode(String name, List<AST> arguments) {
            this.name      = name;
            this.arguments = List.copyOf(arguments);
        }

        @Override public AST_Type getType() { return AST_Type.functionCall; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitFunctionCall(this); }

        @Override public String toString() {
            return "FuncCall{" + name + "(" + arguments + ")}";
        }
    }

    public static class PriorityStatementNode implements AST {

        public final int priority;
        public final AST statement;

        public PriorityStatementNode(int priority, AST statement) {
            this.priority  = priority;
            this.statement = statement;
        }

        @Override public AST_Type getType() { return AST_Type.priorityStatement; }

        @Override public <R> R accept(ASTVisitor<R> v) { return v.visitPriority(this); }

        @Override public String toString() {
            return "Priority{" + priority + ": " + statement + "}";
        }
    }
}