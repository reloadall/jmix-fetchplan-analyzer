package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ForEachStatementHandler")
@Order(160)
public class ForEachStatementHandler implements StatementHandler {

    @Override
    public boolean supports(Statement statement) {
        return statement.isForEachStmt();
    }

    @Override
    public StatementHandleResult handle(RawTree rawTree,
                                        AnalysisStep step,
                                        Statement statement,
                                        EngineContext context) {
        ForEachStmt forEachStmt = statement.asForEachStmt();
        StatementsPayload currentPayload = (StatementsPayload) step.getPayload();
        StatementsPayload afterLoopPayload = currentPayload.next();

        Continuation afterLoopContinuation = new Continuation(
                step.getMethod(),
                afterLoopPayload,
                step.getCurrentRawNode(),
                step.getBindings()
        );

        List<Continuation> continuations = new ArrayList<>();

        if (afterLoopPayload.hasCurrentStatement() || afterLoopPayload.getContinuationOnFinish() != null) {
            continuations.add(afterLoopContinuation);
        }

        RawNode elementNode = resolveElementNode(rawTree, step, forEachStmt, context);
        if (elementNode == null) {
            rawTree.addUnknownBreak(step.getCurrentRawNode(), null, UsageKind.NONE);
            return StatementHandleResult.customContinuations(continuations);
        }

        String loopVariableName = resolveLoopVariableName(forEachStmt);
        Map<String, RawNode> nextBindings = new HashMap<>(step.getBindings());
        nextBindings.put(loopVariableName, elementNode);

        StatementsPayload bodyPayload = new StatementsPayload(
                asStatements(forEachStmt.getBody()),
                0,
                afterLoopContinuation
        );

        continuations.add(step.continueWith(
                step.getMethod(),
                bodyPayload,
                elementNode,
                nextBindings
        ));

        return StatementHandleResult.customContinuations(continuations);
    }

    private RawNode resolveElementNode(RawTree rawTree,
                                       AnalysisStep step,
                                       ForEachStmt forEachStmt,
                                       EngineContext context) {
        Expression iterable = forEachStmt.getIterable();

        RawNode iterableNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                iterable,
                context
        );
        if (iterableNode == null) {
            return null;
        }

        return rawTree.addChild(
                iterableNode,
                null,
                FlowKind.COLLECTION_ELEMENT,
                null,
                UsageKind.INTERMEDIATE
        );
    }

    private String resolveLoopVariableName(ForEachStmt forEachStmt) {
        List<VariableDeclarator> variables = forEachStmt.getVariable().getVariables();
        if (variables.isEmpty()) {
            throw new IllegalStateException("ForEach variable is absent");
        }
        return variables.get(0).getNameAsString();
    }

    private List<Statement> asStatements(Statement statement) {
        if (statement.isBlockStmt()) {
            return statement.asBlockStmt().getStatements();
        }
        return List.of(statement);
    }
}
