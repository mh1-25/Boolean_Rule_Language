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

    private void advance() {
        pos++;
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(currentChar())) advance();
    }

    private String readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar())) {
            sb.append(currentChar());
            advance();
        }
        return sb.toString();
    }

    private String readNumber() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(currentChar())) {
            sb.append(currentChar());
            advance();
        }
        return sb.toString();
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (currentChar() != '\0') {
            skipWhitespace();

            char c = currentChar();

            if (Character.isLetter(c)) {
                String word = readIdentifier();

                switch (word) {
                    case "true": tokens.add(new Token(TokenType.TRUE, word)); break;
                    case "false": tokens.add(new Token(TokenType.FALSE, word)); break;
                    case "and": tokens.add(new Token(TokenType.AND, word)); break;
                    case "or": tokens.add(new Token(TokenType.OR, word)); break;
                    case "not": tokens.add(new Token(TokenType.NOT, word)); break;
                    case "print": tokens.add(new Token(TokenType.PRINT, word)); break;
                    default: tokens.add(new Token(TokenType.IDENTIFIER, word));
                }
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(new Token(TokenType.NUMBER, readNumber()));
                continue;
            }

            switch (c) {
                case '+': tokens.add(new Token(TokenType.PLUS, "+")); break;
                case '-': tokens.add(new Token(TokenType.MINUS, "-")); break;
                case '*': tokens.add(new Token(TokenType.MUL, "*")); break;
                case '/': tokens.add(new Token(TokenType.DIV, "/")); break;
                case '(': tokens.add(new Token(TokenType.LPAREN, "(")); break;
                case ')': tokens.add(new Token(TokenType.RPAREN, ")")); break;
                case ';': tokens.add(new Token(TokenType.SEMICOLON, ";")); break;

                case ':':
                    advance();
                    if (currentChar() == '=') {
                        tokens.add(new Token(TokenType.ASSIGN, ":="));
                    } else throw new RuntimeException("Invalid ':'");
                    break;

                case '>':
                    advance();
                    if (currentChar() == '=') tokens.add(new Token(TokenType.GTE, ">="));
                    else { tokens.add(new Token(TokenType.GT, ">")); continue; }
                    break;

                case '<':
                    advance();
                    if (currentChar() == '=') tokens.add(new Token(TokenType.LTE, "<="));
                    else { tokens.add(new Token(TokenType.LT, "<")); continue; }
                    break;

                case '=':
                    tokens.add(new Token(TokenType.EQ, "="));
                    break;

                case '!':
                    advance();
                    if (currentChar() == '=') tokens.add(new Token(TokenType.NEQ, "!="));
                    else throw new RuntimeException("Invalid '!'");
                    break;

                default:
                    if (Character.isWhitespace(c) || c < 32) {
                        advance();
                    } else {
                        throw new RuntimeException("Unknown char: " + c + " (code " + (int) c + ")");
                    }
            }
            advance();
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }
}