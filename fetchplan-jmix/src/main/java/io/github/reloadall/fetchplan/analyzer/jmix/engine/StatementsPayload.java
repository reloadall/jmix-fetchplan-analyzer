package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

public class StatementsPayload implements StepPayload {

    private final List<Statement> statements;
    private final int statementIndex;
    private final Continuation continuationOnFinish;

    public static StatementsPayload from(MethodDeclaration method) {
        Objects.requireNonNull(method, "method is null");

        List<Statement> statements = method.getBody()
                .orElseThrow(() -> new IllegalArgumentException("Method body is absent"))
                .getStatements();

        return new StatementsPayload(statements, 0, null);
    }

    public StatementsPayload(List<Statement> statements,
                             int statementIndex,
                             Continuation continuationOnFinish) {
        this.statements = Objects.requireNonNull(statements, "statements is null");
        this.statementIndex = statementIndex;
        this.continuationOnFinish = continuationOnFinish;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public int getStatementIndex() {
        return statementIndex;
    }

    public Continuation getContinuationOnFinish() {
        return continuationOnFinish;
    }

    public boolean hasCurrentStatement() {
        return statementIndex < statements.size();
    }

    public Statement currentStatement() {
        if (!hasCurrentStatement()) {
            throw new IllegalStateException("No current statement");
        }
        return statements.get(statementIndex);
    }

    public StatementsPayload next() {
        return new StatementsPayload(statements, statementIndex + 1, continuationOnFinish);
    }

    public StatementsPayload withContinuationOnFinish(Continuation continuationOnFinish) {
        return new StatementsPayload(statements, statementIndex, continuationOnFinish);
    }
}
