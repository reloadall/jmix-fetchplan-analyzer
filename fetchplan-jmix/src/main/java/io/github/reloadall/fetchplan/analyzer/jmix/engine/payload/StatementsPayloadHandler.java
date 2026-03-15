package io.github.reloadall.fetchplan.analyzer.jmix.engine.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StepPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StepPayloadHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.StatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_StatementsPayloadHandler")
@Order(100)
public class StatementsPayloadHandler implements StepPayloadHandler {

    private final List<StatementHandler> statementHandlers;

    @Autowired
    public StatementsPayloadHandler(List<StatementHandler> statementHandlers) {
        this.statementHandlers = Objects.requireNonNull(statementHandlers, "statementHandlers is null");
    }

    @Override
    public boolean supports(StepPayload payload) {
        return payload instanceof StatementsPayload;
    }

    @Override
    public List<Continuation> handle(RawTree rawTree,
                                     AnalysisStep step,
                                     EngineContext context) {
        StatementsPayload payload = (StatementsPayload) step.getPayload();

        if (!payload.hasCurrentStatement()) {
            if (payload.getContinuationOnFinish() != null) {
                return List.of(payload.getContinuationOnFinish());
            }
            return List.of();
        }

        Statement statement = payload.currentStatement();
        StatementHandler handler = findHandler(statement);

        StatementHandleResult result;
        if (handler == null) {
            result = StatementHandleResult.continueLinear();
        } else {
            result = handler.handle(rawTree, step, statement, context);
        }

        List<Continuation> continuations = new ArrayList<>(result.getContinuations());

        if (result.isContinueLinear()) {
            StatementsPayload nextPayload = payload.next();
            if (nextPayload.hasCurrentStatement() || nextPayload.getContinuationOnFinish() != null) {
                continuations.add(step.continueWith(nextPayload));
            }
        }

        return continuations;
    }

    private StatementHandler findHandler(Statement statement) {
        for (StatementHandler handler : statementHandlers) {
            if (handler.supports(statement)) {
                return handler;
            }
        }
        return null;
    }
}
