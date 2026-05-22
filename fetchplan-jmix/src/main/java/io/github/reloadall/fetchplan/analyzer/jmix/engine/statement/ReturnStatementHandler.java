package io.github.reloadall.fetchplan.analyzer.jmix.engine.statement;

import java.util.List;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementHandleResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
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
        boolean returnsToCaller = returnsToCaller(step);

        if (returnStmt.getExpression().isEmpty()) {
            return returnToCallerIfNeeded(step);
        }

        Expression expression = returnStmt.getExpression().get();

        ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                expression,
                context
        );

        boolean boundaryArgumentUsage = false;
        if (result.isEmpty()) {
            result = collectBoundaryArgumentUsages(rawTree, step, expression, context);
            boundaryArgumentUsage = !result.isEmpty();
        }

        if (result.isEmpty() && unknownBreakPolicy.shouldCreateForReturnFailure(expression)) {
            RawNode breakNode = rawTree.addUnknownBreak(
                    step.getCurrentRawNode(),
                    null,
                    returnsToCaller ? UsageKind.INTERMEDIATE : UsageKind.TERMINAL
            );
            breakNode.setUsageKind(returnsToCaller ? UsageKind.INTERMEDIATE : UsageKind.TERMINAL);
            return returnToCallerIfNeeded(step);
        }

        if (!returnsToCaller || boundaryArgumentUsage) {
            for (RawNode node : result.getNodes()) {
                node.setUsageKind(UsageKind.TERMINAL);
            }
        }

        return returnToCallerIfNeeded(step);
    }

    private ExpressionResolutionResult collectBoundaryArgumentUsages(RawTree rawTree,
                                                                    AnalysisStep step,
                                                                    Expression expression,
                                                                    EngineContext context) {
        if (expression == null) {
            return ExpressionResolutionResult.empty();
        }

        if (expression.isNameExpr()) {
            ValueBinding binding = step.getBinding(expression.asNameExpr().getNameAsString());
            if (binding != null && binding.isTerminalOnly() && !binding.isEmpty()) {
                return new ExpressionResolutionResult(binding.getNodes(), binding.isUncertain());
            }
        }

        if (expression.isMethodCallExpr()) {
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
            for (Expression argument : methodCallExpr.getArguments()) {
                ExpressionResolutionResult argumentResult = context.getExpressionResolver().resolveAll(
                        rawTree,
                        step,
                        argument,
                        context
                );

                if (argumentResult.isEmpty()) {
                    argumentResult = collectBoundaryArgumentUsages(rawTree, step, argument, context);
                }

                merged = merged.merge(argumentResult);
            }
            return merged;
        }

        if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
            ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
            for (Expression argument : objectCreationExpr.getArguments()) {
                ExpressionResolutionResult argumentResult = context.getExpressionResolver().resolveAll(
                        rawTree,
                        step,
                        argument,
                        context
                );

                if (argumentResult.isEmpty()) {
                    argumentResult = collectBoundaryArgumentUsages(rawTree, step, argument, context);
                }

                merged = merged.merge(argumentResult);
            }
            return merged;
        }

        return ExpressionResolutionResult.empty();
    }

    private boolean returnsToCaller(AnalysisStep step) {
        if (!(step.getPayload() instanceof StatementsPayload statementsPayload)) {
            return false;
        }

        return returnsAcrossMethodBoundary(step, statementsPayload);
    }

    private StatementHandleResult returnToCallerIfNeeded(AnalysisStep step) {
        if (step.getPayload() instanceof StatementsPayload statementsPayload) {
            Continuation continuationOnFinish = statementsPayload.getContinuationOnFinish();
            if (continuationOnFinish != null && returnsAcrossMethodBoundary(step, statementsPayload)) {
                return StatementHandleResult.customContinuations(List.of(continuationOnFinish));
            }
        }

        return StatementHandleResult.stop();
    }

    private boolean returnsAcrossMethodBoundary(AnalysisStep step, StatementsPayload payload) {
        Continuation continuationOnFinish = payload.getContinuationOnFinish();

        while (continuationOnFinish != null) {
            if (continuationOnFinish.getMethod() != step.getMethod()) {
                return true;
            }

            if (!(continuationOnFinish.getPayload() instanceof StatementsPayload nextPayload)) {
                return false;
            }

            continuationOnFinish = nextPayload.getContinuationOnFinish();
        }

        return false;
    }

    private boolean isTopLevelMethodBody(AnalysisStep step, StatementsPayload payload) {
        if (step.getMethod().getBody().isEmpty()) {
            return false;
        }

        return payload.getStatements() == step.getMethod().getBody().get().getStatements();
    }
}
