package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ConditionalExpressionHandler")
@Order(175)
public class ConditionalExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        return expression.isConditionalExpr();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        ConditionalExpr conditionalExpr = expression.asConditionalExpr();

        Expression thenExpr = conditionalExpr.getThenExpr();
        Expression elseExpr = conditionalExpr.getElseExpr();

        boolean thenIsNull = thenExpr.isNullLiteralExpr();
        boolean elseIsNull = elseExpr.isNullLiteralExpr();

        ExpressionResolutionResult thenResult = thenIsNull
                ? ExpressionResolutionResult.empty()
                : context.getExpressionResolver().resolveAll(rawTree, step, thenExpr, context);

        ExpressionResolutionResult elseResult = elseIsNull
                ? ExpressionResolutionResult.empty()
                : context.getExpressionResolver().resolveAll(rawTree, step, elseExpr, context);

        if (thenIsNull && !elseResult.isEmpty()) {
            return elseResult;
        }

        if (elseIsNull && !thenResult.isEmpty()) {
            return thenResult;
        }

        if (thenIsNull && elseIsNull) {
            return ExpressionResolutionResult.empty();
        }

        return thenResult.merge(elseResult);
    }

}
