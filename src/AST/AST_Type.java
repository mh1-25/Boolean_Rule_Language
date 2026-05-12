package AST;

public enum AST_Type {
    program,
    identifier,
    binaryExpression,
    unaryExpression,
    NumericalLiteral,
    BooleanLiteral,
    printStatement,
    assignmentStatement,
    functionCall,
    priorityStatement,
}