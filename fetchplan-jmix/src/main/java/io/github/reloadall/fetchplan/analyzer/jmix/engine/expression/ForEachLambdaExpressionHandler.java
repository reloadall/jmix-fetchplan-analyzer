package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_ForEachLambdaExpressionHandler")
@Order(165)
public class ForEachLambdaExpressionHandler implements ExpressionHandler {

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return "forEach".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && methodCallExpr.getArguments().size() == 1
                && methodCallExpr.getArgument(0).isLambdaExpr()
                && methodCallExpr.getArgument(0).asLambdaExpr().getParameters().size() == 1;
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        LambdaExpr lambdaExpr = methodCallExpr.getArgument(0).asLambdaExpr();

        LambdaElementBindingSupport.ScopeElements scopeElements = lambdaSupport.resolveScopeElements(
                rawTree,
                step,
                methodCallExpr,
                context
        );
        ExpressionResolutionResult scopeResult = scopeElements.scopeResult();
        if (scopeResult.isEmpty()) {
            return scopeResult;
        }

        AnalysisStep lambdaStep = lambdaSupport.createLambdaStep(
                step,
                lambdaExpr,
                scopeElements.elementNodes(),
                scopeResult.isUncertain()
        );
        ExpressionResolutionResult bodyResult = lambdaSupport.resolveLambdaBody(rawTree, lambdaStep, lambdaExpr, context);
        markTerminal(bodyResult);
        return new ExpressionResolutionResult(bodyResult.getNodes(), scopeResult.isUncertain() || bodyResult.isUncertain());
    }

    private void markTerminal(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }
}