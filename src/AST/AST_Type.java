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
    functionCall,        // aggregation: sum(), min(), max(), count(), avg(), any(), all()
    priorityStatement,   // priority N statement
}