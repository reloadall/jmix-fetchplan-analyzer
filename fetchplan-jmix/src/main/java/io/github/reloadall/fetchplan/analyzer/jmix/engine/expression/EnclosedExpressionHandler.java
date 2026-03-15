package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_EnclosedExpressionHandler")
@Order(300)
public class EnclosedExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        return expression.isEnclosedExpr();
    }

    @Override
    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
        return context.getExpressionResolver().resolve(
                rawTree,
                step,
                enclosedExpr.getInner(),
                context
        );
    }

}
