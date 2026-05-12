package AST;

import Token.Token;
import Token.TokenType;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token current() {
        return tokens.get(pos);
    }

    private Token peek(int offset) {
        int idx = pos + offset;
        if (idx >= tokens.size()) return tokens.get(tokens.size() - 1);
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


    public Nodes.ProgramNode parse() {
        List<AST> statements = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        return new Nodes.ProgramNode(statements);
    }

    private AST parseStatement() {

        if (check(TokenType.PRIORITY)) {
            return parsePriorityStatement();
        }
        if (check(TokenType.PRINT)) {
            return parsePrintStatement();
        }
        if (check(TokenType.IDENTIFIER) && peek(1).getType() == TokenType.ASSIGN) {
            return parseAssignmentStatement();
        }
        throw new RuntimeException(
            "Syntax error: unexpected token '" + current().getValue() +
            "' at position " + pos + ". Expected a statement."
        );
    }

    private AST parsePriorityStatement() {
        expect(TokenType.PRIORITY);
        Token numTok = expect(TokenType.NUMBER);

        double val = Double.parseDouble(numTok.getValue());
        if (val != Math.floor(val) || val < 0) {
            throw new RuntimeException(
                "Syntax error: priority must be a non-negative integer, got '" + numTok.getValue() + "'"
            );
        }
        int priority = (int) val;

        AST stmt = parseStatement();
        return new Nodes.PriorityStatementNode(priority, stmt);
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

    private AST parseExpression() {
        return parseOrExpr();
    }

    private AST parseOrExpr() {
        AST left = parseAndExpr();
        while (check(TokenType.OR)) {
            String op = consume().getValue();
            AST right = parseAndExpr();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    private AST parseAndExpr() {
        AST left = parseNotExpr();
        while (check(TokenType.AND)) {
            String op = consume().getValue();
            AST right = parseNotExpr();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    private AST parseNotExpr() {
        if (check(TokenType.NOT)) {
            String op = consume().getValue();
            AST operand = parseNotExpr();
            return new Nodes.UnaryExpressionNode(op, operand);
        }
        return parseComparison();
    }

    private AST parseComparison() {
        AST left = parseAddition();
        if (isComparisonOperator(current().getType())) {
            String op = consume().getValue();
            AST right = parseAddition();
            AST result = new Nodes.BinaryExpressionNode(left, op, right);

            if (isComparisonOperator(current().getType())) {
                throw new RuntimeException(
                    "Syntax error: chained comparison '" + current().getValue() +
                    "' is not allowed. Use 'and' to combine: (a < b) and (b < c)"
                );
            }
            return result;
        }
        return left;
    }

    private boolean isComparisonOperator(TokenType t) {
        return t == TokenType.EQ  || t == TokenType.NEQ ||
               t == TokenType.LT  || t == TokenType.GT  ||
               t == TokenType.LTE || t == TokenType.GTE;
    }

    private AST parseAddition() {
        AST left = parseMultiply();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = consume().getValue();
            AST right = parseMultiply();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    private AST parseMultiply() {
        AST left = parseUnary();
        while (check(TokenType.MUL) || check(TokenType.DIV)) {
            String op = consume().getValue();
            AST right = parseUnary();
            left = new Nodes.BinaryExpressionNode(left, op, right);
        }
        return left;
    }

    private AST parseUnary() {
        if (check(TokenType.MINUS)) {
            String op = consume().getValue();
            AST operand = parseUnary();
            return new Nodes.UnaryExpressionNode(op, operand);
        }
        return parsePrimary();
    }

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
            if (peek(1).getType() == TokenType.LPAREN) {
                consume();
                return parseFunctionCall(t.getValue());
            }
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

    private AST parseFunctionCall(String name) {
        expect(TokenType.LPAREN);
        List<AST> args = new ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            args.add(parseExpression());
            while (match(TokenType.COMMA)) {
                args.add(parseExpression());
            }
        }

        expect(TokenType.RPAREN);
        return new Nodes.FunctionCallNode(name, args);
    }
}