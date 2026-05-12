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

public interface ASTVisitor<R> {

    R visitProgram(ProgramNode node);

    R visitAssignment(AssignmentNode node);

    R visitPrint(PrintNode node);

    R visitBinaryExpression(BinaryExpressionNode node);

    R visitUnaryExpression(UnaryExpressionNode node);

    R visitNumber(NumberLiteralNode node);

    R visitBoolean(BooleanLiteralNode node);

    R visitIdentifier(IdentifierNode node);
    
    R visitFunctionCall(FunctionCallNode node);

    R visitPriority(PriorityStatementNode node);
}