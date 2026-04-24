import java.util.List;

import Token.Lexer;
import Token.Token;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
        String code = "x := 5; y := 10; print(x + y);";
         Lexer lexer = new Lexer(code);
        
        List<Token> tokens = lexer.tokenize();

        System.out.println("=== TOKENS ===");
        tokens.forEach(System.out::println);
    }
}
