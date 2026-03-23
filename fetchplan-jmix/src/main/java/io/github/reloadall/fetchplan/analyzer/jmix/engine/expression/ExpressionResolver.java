package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
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

        ExpressionResolutionResult merged = ExpressionResolutionResult.empty();
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

            if (!result.isEmpty() || result.isUncertain()) {
                merged = merged.merge(result);

                // Для большинства кейсов можно возвращать сразу первый осмысленный результат.
                // Но merge оставлен, чтобы не потерять union-compatible handlers.
                return merged;
            }
        }

        return supportedByAny ? merged : ExpressionResolutionResult.empty();
    }

    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        ExpressionResolutionResult result = resolveAll(rawTree, step, expression, context);
        return result.getSingleNodeOrNull();
    }

}
