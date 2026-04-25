package AST;

import java.util.ArrayList;
import java.util.List;

import Token.Token;
import Token.TokenType;

/**
 * Recursive-descent parser.
 *
 * GRAMMAR (operator precedence, lowest → highest binding)
 * ─────────────────────────────────────────────────────────
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
 * addition   → multiply ( ('+'|'-') multiply )*
 * multiply   → unary    ( ('*'|'/') unary    )*
 * unary      → '-' unary | primary
 * primary    → NUMBER | 'true' | 'false' | IDENTIFIER | '(' expression ')'
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ─────────────────────────────────────────────
    // Token navigation helpers
    // ─────────────────────────────────────────────

    private Token current() {
        return tokens.get(pos);
    }

    private Token peek(int offset) {
        int idx = pos + offset;
        if (idx >= tokens.size()) return tokens.get(tokens.size() - 1); // EOF
        return tokens.get(idx);
    }

    private Token consume() {
        Token t = tokens.get(pos);
        pos++;
        return t;
    }

    private Token expect(TokenType type) {
        if (current().getType() != type) {
            throw new RuntimeException(
                "Syntax error: expected " + type +
                " but got " + current().getType() +
                " ('" + current().getValue() + "') at token position " + pos
            );
        }
        return consume();
    }

    private boolean check(TokenType type) {
        return current().getType() == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) { consume(); return true; }
        return false;
    }

    // ─────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────

    public Nodes.ProgramNode parse() {
        List<AST> statements = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        return new Nodes.ProgramNode(statements);
    }

    // ─────────────────────────────────────────────
    // Statements
    // ─────────────────────────────────────────────

    private AST parseStatement() {
        // print expression ;
        if (check(TokenType.PRINT)) {
            return parsePrintStatement();
        }
        // IDENTIFIER ':=' expression ;
        if (check(TokenType.IDENTIFIER) && peek(1).getType() == TokenType.ASSIGN) {
            return parseAssignmentStatement();
        }
        throw new RuntimeException(
            "Syntax error: unexpected token '" + current().getValue() +
            "' at position " + pos + ". Expected a statement."
        );
    }

    private AST parsePrintStatement() {
        expect(TokenType.PRINT);
        AST expr = parseExpression();
        expect(TokenType.SEMICOLON);
        return new Nodes.PrintNode(expr);
    }

    private AST parseAssignmentStatement() {
        Token id = expect(TokenType.IDENTIFIER);
        expect(TokenType.ASSIGN);
        AST value = parseExpression();
        expect(TokenType.SEMICOLON);
        return new Nodes.AssignmentNode(id.getValue(), value);
    }

    // ─────────────────────────────────────────────
    // Expressions — following the grammar precisely
    // ─────────────────────────────────────────────

    /** expression → orExpr */
    private AST parseExpression() {
        return parseOrExpr();
    }

    /** orExpr → andExpr ( 'or' andExpr )* */
    private AST parseOrExpr() {
        AST left = parseAndExpr();
        while (check(TokenType.OR)) {
            String op = consume().getValue();
            AST right = parseAndExpr();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    /** andExpr → notExpr ( 'and' notExpr )* */
    private AST parseAndExpr() {
        AST left = parseNotExpr();
        while (check(TokenType.AND)) {
            String op = consume().getValue();
            AST right = parseNotExpr();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    /** notExpr → 'not' notExpr | comparison */
    private AST parseNotExpr() {
        if (check(TokenType.NOT)) {
            String op = consume().getValue();
            AST operand = parseNotExpr();
            return new Nodes.UnaryExpressionNode(op, operand);
        }
        return parseComparison();
    }

    /** comparison → addition ( ('='|'!='|'<'|'>'|'<='|'>=') addition )? */
    private AST parseComparison() {
        AST left = parseAddition();
        if (isComparisonOperator(current().getType())) {
            String op = consume().getValue();
            AST right = parseAddition();
            return new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    private boolean isComparisonOperator(TokenType t) {
        return t == TokenType.EQ  || t == TokenType.NEQ ||
               t == TokenType.LT  || t == TokenType.GT  ||
               t == TokenType.LTE || t == TokenType.GTE;
    }

    /** addition → multiply ( ('+'|'-') multiply )* */
    private AST parseAddition() {
        AST left = parseMultiply();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = consume().getValue();
            AST right = parseMultiply();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    /** multiply → unary ( ('*'|'/') unary )* */
    private AST parseMultiply() {
        AST left = parseUnary();
        while (check(TokenType.MUL) || check(TokenType.DIV)) {
            String op = consume().getValue();
            AST right = parseUnary();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    /** unary → '-' unary | primary */
    private AST parseUnary() {
        if (check(TokenType.MINUS)) {
            String op = consume().getValue();
            AST operand = parseUnary();
            return new Nodes.UnaryExpressionNode(op, operand);
        }
        return parsePrimary();
    }

    /** primary → NUMBER | 'true' | 'false' | IDENTIFIER | '(' expression ')' */
    private AST parsePrimary() {
        Token t = current();

        if (t.getType() == TokenType.NUMBER) {
            consume();
            return new Nodes.NumberLiteralNode(Double.parseDouble(t.getValue()));
        }

        if (t.getType() == TokenType.TRUE) {
            consume();
            return new Nodes.BooleanLiteralNode(true);
        }

        if (t.getType() == TokenType.FALSE) {
            consume();
            return new Nodes.BooleanLiteralNode(false);
        }

        if (t.getType() == TokenType.IDENTIFIER) {
            consume();
            return new Nodes.IdentifierNode(t.getValue());
        }

        if (t.getType() == TokenType.LPAREN) {
            consume();
            AST expr = parseExpression();
            expect(TokenType.RPAREN);
            return expr;
        }

        throw new RuntimeException(
            "Syntax error: unexpected token '" + t.getValue() +
            "' (" + t.getType() + ") at position " + pos +
            ". Expected a number, boolean, identifier, or '('."
        );
    }
}