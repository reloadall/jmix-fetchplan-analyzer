package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_StreamFlatMapLambdaExpressionHandler")
@Order(162)
public class StreamFlatMapLambdaExpressionHandler implements ExpressionHandler {

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return "flatMap".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && methodCallExpr.getArguments().size() == 1
                && methodCallExpr.getArgument(0).isLambdaExpr()
                && methodCallExpr.getArgument(0).asLambdaExpr().getParameters().size() == 1
                && StreamCollectorSupport.isSupportedReturnLikeLambdaBody(methodCallExpr.getArgument(0).asLambdaExpr());
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
        LambdaElementBindingSupport.LambdaReturnResult lambdaReturnResult = lambdaSupport.resolveLambdaReturnBody(
                rawTree,
                lambdaStep,
                lambdaExpr,
                context
        );
        StreamCollectorSupport.markTerminal(lambdaReturnResult.preReturnReads());

        ExpressionResolutionResult returnedResult = lambdaReturnResult.returnedResult();
        Set<RawNode> flatMappedElementNodes = new LinkedHashSet<>();
        for (RawNode returnedNode : returnedResult.getNodes()) {
            RawNode elementNode = rawTree.addChild(
                    returnedNode,
                    null,
                    FlowKind.COLLECTION_ELEMENT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            flatMappedElementNodes.add(elementNode);
        }

        return new ExpressionResolutionResult(
                flatMappedElementNodes,
                scopeResult.isUncertain()
                        || returnedResult.isUncertain()
                        || lambdaReturnResult.preReturnReads().isUncertain()
        );
    }
}