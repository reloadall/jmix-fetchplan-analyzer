package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.List;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.UnknownBreakPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ReturnStatementHandler")
@Order(200)
public class ReturnStatementHandler implements StatementHandler {

    private final UnknownBreakPolicy unknownBreakPolicy;

    @Autowired
    public ReturnStatementHandler(UnknownBreakPolicy unknownBreakPolicy) {
        this.unknownBreakPolicy = unknownBreakPolicy;
    }

    @Override
    public boolean supports(Statement statement) {
        return statement.isReturnStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        ReturnStmt returnStmt = statement.asReturnStmt();

        if (returnStmt.getExpression().isEmpty()) {
            return returnToCallerIfNeeded(step);
        }

        Expression expression = returnStmt.getExpression().get();
        RawNode resolvedNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                expression,
                context
        );

        if (resolvedNode == null && unknownBreakPolicy.shouldCreateForReturnFailure(expression)) {
            resolvedNode = rawTree.addUnknownBreak(
                    step.getCurrentRawNode(),
                    null,
                    UsageKind.TERMINAL
            );
        }

        if (resolvedNode != null) {
            resolvedNode.setUsageKind(UsageKind.TERMINAL);
        }

        return returnToCallerIfNeeded(step);
    }

    private StatementHandleResult returnToCallerIfNeeded(AnalysisStep step) {
        if (step.getPayload() instanceof StatementsPayload statementsPayload) {
            Continuation continuationOnFinish = statementsPayload.getContinuationOnFinish();
            if (continuationOnFinish != null && isTopLevelMethodBody(step, statementsPayload)) {
                return StatementHandleResult.customContinuations(List.of(continuationOnFinish));
            }
        }

        return StatementHandleResult.stop();
    }

    private boolean isTopLevelMethodBody(AnalysisStep step, StatementsPayload payload) {
        if (step.getMethod().getBody().isEmpty()) {
            return false;
        }

        return payload.getStatements() == step.getMethod().getBody().get().getStatements();
    }
}
