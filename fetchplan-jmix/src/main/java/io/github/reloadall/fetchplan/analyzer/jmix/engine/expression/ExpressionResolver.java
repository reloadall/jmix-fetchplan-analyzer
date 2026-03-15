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

    private final List<ExpressionHandler> handlers;

    @Autowired
    public ExpressionResolver(List<ExpressionHandler> handlers) {
        this.handlers = Objects.requireNonNull(handlers, "handlers is null");
    }

    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        if (expression == null) {
            return null;
        }

        for (ExpressionHandler handler : handlers) {
            if (handler.supports(expression)) {
                return handler.resolve(rawTree, step, expression, context);
            }
        }

        return null;
    }
}
