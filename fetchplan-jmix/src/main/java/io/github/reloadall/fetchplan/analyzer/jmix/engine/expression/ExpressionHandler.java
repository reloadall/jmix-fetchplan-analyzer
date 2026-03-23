package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;

public interface ExpressionHandler {

    boolean supports(Expression expression);

    /**
     * Transitional single-result API.
     * Existing handlers may keep implementing this method until they are migrated to resolveAll(...).
     */
    default RawNode resolve(RawTree rawTree,
                            AnalysisStep step,
                            Expression expression,
                            EngineContext context) {
        ExpressionResolutionResult result = resolveAll(rawTree, step, expression, context);
        return result.getSingleNodeOrNull();
    }

    /**
     * New multi-result API.
     * Default implementation preserves backward compatibility for legacy single-result handlers.
     */
    default ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                  AnalysisStep step,
                                                  Expression expression,
                                                  EngineContext context) {
        RawNode node = resolve(rawTree, step, expression, context);
        return node != null
                ? ExpressionResolutionResult.of(node)
                : ExpressionResolutionResult.empty();
    }

}
