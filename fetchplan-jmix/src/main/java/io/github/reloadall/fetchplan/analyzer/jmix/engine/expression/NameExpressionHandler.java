package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_NameExpressionHandler")
@Order(100)
public class NameExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        return expression.isNameExpr();
    }

    @Override
    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        NameExpr nameExpr = expression.asNameExpr();
        return step.resolveBinding(nameExpr.getNameAsString());
    }
}
