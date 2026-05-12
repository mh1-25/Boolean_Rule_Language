package Token;

public enum TokenType {
    IDENTIFIER, NUMBER,

    TRUE, FALSE,
    AND, OR, NOT,
    PRINT,
    PRIORITY,

    PLUS, MINUS, MUL, DIV,
    LT, GT, LTE, GTE, EQ, NEQ,

    ASSIGN,

    LPAREN, RPAREN, SEMICOLON, COMMA,
    EOF
}
