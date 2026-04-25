package AST;

public enum AST_Type {
    program,
    identifier,
    binaryExpression,
    unaryExpression,       // NEW: 'not' and unary minus '-'
    NumericalLiteral,
    BooleanLiteral,
    printStatement,
    assignmentStatement,
}