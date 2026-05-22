package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.stereotype.Component;

@Component("fpa_ExpressionResolver")
public class ExpressionResolver {

    private final List<ExpressionHandler> expressionHandlers;

    public ExpressionResolver(List<ExpressionHandler> expressionHandlers) {
        this.expressionHandlers = Objects.requireNonNull(expressionHandlers, "expressionHandlers is null");
    }

    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        if (expression == null) {
            return ExpressionResolutionResult.empty();
        }

        boolean supportedByAny = false;

        for (ExpressionHandler handler : expressionHandlers) {
            if (!handler.supports(expression)) {
                continue;
            }

            supportedByAny = true;

            ExpressionResolutionResult result = handler.resolveAll(
                    rawTree,
                    step,
                    expression,
                    context
            );

            // Resolution strategy is intentionally order-sensitive.
            // The first handler that produces a meaningful result wins:
            // - non-empty result; or
            // - uncertain result.
            //
            // This resolver does not aggregate results from later compatible handlers.
            // Any broader merge semantics must be introduced explicitly and backed by
            // dedicated tests, because handler order is observable analyzer behavior.
            if (!result.isEmpty() || result.isUncertain()) {
                return result;
            }
        }

        return ExpressionResolutionResult.empty();
    }

    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        ExpressionResolutionResult result = resolveAll(rawTree, step, expression, context);
        return result.getSingleNodeOrNull();
    }

}
