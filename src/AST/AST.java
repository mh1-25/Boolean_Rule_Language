package AST;


public interface AST {

    AST_Type getType();

    <R> R accept(ASTVisitor<R> visitor);

    default boolean isStatement() {
        return switch (getType()) {
            case program, assignmentStatement, printStatement, priorityStatement -> true;
            default -> false;
        };
    }

    default boolean isExpression() {
        return !isStatement();
    }

    @Override
    String toString();
}