package Token;

import java.util.*;

public class Lexer {
    private String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input;
    }

    private char currentChar() {
        if (pos >= input.length()) return '\0';
        return input.charAt(pos);
    }

    private void advance() { pos++; }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(currentChar())) advance();
    }

    private String readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && Character.isLetterOrDigit(currentChar())) {
            sb.append(currentChar());
            advance();
        }
        return sb.toString();
    }

    private String readNumber() {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && Character.isDigit(currentChar())) {
            sb.append(currentChar());
            advance();
        }
        if (pos < input.length() && currentChar() == '.') {
            sb.append('.');
            advance();
            while (pos < input.length() && Character.isDigit(currentChar())) {
                sb.append(currentChar());
                advance();
            }
        }
        return sb.toString();
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) break;

            char c = currentChar();

            if (Character.isLetter(c)) {
                String word = readIdentifier();
                switch (word) {
                    case "true"  -> tokens.add(new Token(TokenType.TRUE, word));
                    case "false" -> tokens.add(new Token(TokenType.FALSE, word));
                    case "and"   -> tokens.add(new Token(TokenType.AND, word));
                    case "or"    -> tokens.add(new Token(TokenType.OR, word));
                    case "not"   -> tokens.add(new Token(TokenType.NOT, word));
                    case "print" -> tokens.add(new Token(TokenType.PRINT, word));
                    default      -> tokens.add(new Token(TokenType.IDENTIFIER, word));
                }
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(new Token(TokenType.NUMBER, readNumber()));
                continue;
            }

            advance(); 
            switch (c) {
                case '+' -> tokens.add(new Token(TokenType.PLUS,      "+"));
                case '-' -> tokens.add(new Token(TokenType.MINUS,     "-"));
                case '*' -> tokens.add(new Token(TokenType.MUL,       "*"));
                case '/' -> tokens.add(new Token(TokenType.DIV,       "/"));
                case '(' -> tokens.add(new Token(TokenType.LPAREN,    "("));
                case ')' -> tokens.add(new Token(TokenType.RPAREN,    ")"));
                case ';' -> tokens.add(new Token(TokenType.SEMICOLON, ";"));
                case '=' -> tokens.add(new Token(TokenType.EQ,        "="));

                case ':' -> {
                    if (currentChar() == '=') { advance(); tokens.add(new Token(TokenType.ASSIGN, ":=")); }
                    else throw new RuntimeException("Lexer error: expected '=' after ':', got '" + currentChar() + "'");
                }
                case '>' -> {
                    if (currentChar() == '=') { advance(); tokens.add(new Token(TokenType.GTE, ">=")); }
                    else tokens.add(new Token(TokenType.GT, ">"));
                }
                case '<' -> {
                    if (currentChar() == '=') { advance(); tokens.add(new Token(TokenType.LTE, "<=")); }
                    else tokens.add(new Token(TokenType.LT, "<"));
                }
                case '!' -> {
                    if (currentChar() == '=') { advance(); tokens.add(new Token(TokenType.NEQ, "!=")); }
                    else throw new RuntimeException("Lexer error: expected '=' after '!', got '" + currentChar() + "'");
                }
                default -> throw new RuntimeException(
                    "Lexer error: unknown character '" + c + "' (code " + (int) c + ") at position " + (pos - 1)
                );
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}