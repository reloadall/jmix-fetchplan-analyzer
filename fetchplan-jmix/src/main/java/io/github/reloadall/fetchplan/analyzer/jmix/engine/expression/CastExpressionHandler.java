package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_CastExpressionHandler")
@Order(400)
public class CastExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        return expression.isCastExpr();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        CastExpr castExpr = expression.asCastExpr();

        return context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                castExpr.getExpression(),
                context
        );
    }

}
