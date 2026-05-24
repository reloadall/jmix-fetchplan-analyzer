package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.PassThroughMethodPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_PassThroughMethodCallExpressionHandler")
@Order(180)
public class PassThroughMethodCallExpressionHandler implements ExpressionHandler {

    private final PassThroughMethodPolicy passThroughMethodPolicy;

    @Autowired
    public PassThroughMethodCallExpressionHandler(PassThroughMethodPolicy passThroughMethodPolicy) {
        this.passThroughMethodPolicy = passThroughMethodPolicy;
    }

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return passThroughMethodPolicy.isPassThrough(methodCallExpr);
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

        if (methodCallExpr.getScope().isEmpty()) {
            return ExpressionResolutionResult.empty();
        }

        return context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );
    }
}
