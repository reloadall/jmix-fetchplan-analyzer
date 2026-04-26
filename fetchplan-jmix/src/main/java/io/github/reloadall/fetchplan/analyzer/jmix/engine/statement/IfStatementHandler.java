package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_IfStatementHandler")
@Order(150)
public class IfStatementHandler implements StatementHandler {

    @Override
    public boolean supports(Statement statement) {
        return statement.isIfStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        IfStmt ifStmt = statement.asIfStmt();
        markConditionUsage(rawTree, step, ifStmt, context);

        StatementsPayload currentPayload = (StatementsPayload) step.getPayload();

        StatementsPayload afterIfPayload = currentPayload.next();
        Continuation afterIfContinuation = new Continuation(
                step.getMethod(),
                afterIfPayload,
                step.getCurrentRawNode(),
                step.getBindings()
        );

        List<Continuation> continuations = new ArrayList<>();

        StatementsPayload thenPayload = new StatementsPayload(
                asStatements(ifStmt.getThenStmt()),
                0,
                afterIfContinuation
        );
        continuations.add(step.continueWith(thenPayload));

        if (ifStmt.getElseStmt().isPresent()) {
            StatementsPayload elsePayload = new StatementsPayload(
                    asStatements(ifStmt.getElseStmt().get()),
                    0,
                    afterIfContinuation
            );
            continuations.add(step.continueWith(elsePayload));
        } else {
            if (afterIfPayload.hasCurrentStatement() || afterIfPayload.getContinuationOnFinish() != null) {
                continuations.add(afterIfContinuation);
            }
        }

        return StatementHandleResult.customContinuations(continuations);
    }

    private void markConditionUsage(RawTree rawTree,
                                    AnalysisStep step,
                                    IfStmt ifStmt,
                                    EngineContext context) {
        ExpressionResolutionResult conditionResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                ifStmt.getCondition(),
                context
        );

        for (RawNode node : conditionResult.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }

    private List<Statement> asStatements(Statement statement) {
        if (statement.isBlockStmt()) {
            return statement.asBlockStmt().getStatements();
        }
        return List.of(statement);
    }

}
