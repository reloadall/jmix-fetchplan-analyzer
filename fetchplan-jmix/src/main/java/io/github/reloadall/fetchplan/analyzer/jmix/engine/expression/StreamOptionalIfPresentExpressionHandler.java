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

@Component("fpa_StreamOptionalIfPresentExpressionHandler")
@Order(171)
public class StreamOptionalIfPresentExpressionHandler implements ExpressionHandler {

    private static final Set<String> SUPPORTED_FIND_METHODS = Set.of("findFirst", "findAny");

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr ifPresentCall = expression.asMethodCallExpr();
        if (!"ifPresent".equals(ifPresentCall.getNameAsString())
                || ifPresentCall.getScope().isEmpty()
                || ifPresentCall.getArguments().size() != 1
                || !ifPresentCall.getArgument(0).isLambdaExpr()
                || ifPresentCall.getArgument(0).asLambdaExpr().getParameters().size() != 1
                || !ifPresentCall.getScope().get().isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr findCall = ifPresentCall.getScope().get().asMethodCallExpr();
        return SUPPORTED_FIND_METHODS.contains(findCall.getNameAsString())
                && findCall.getScope().isPresent()
                && findCall.getArguments().isEmpty();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr ifPresentCall = expression.asMethodCallExpr();
        MethodCallExpr findCall = ifPresentCall.getScope().get().asMethodCallExpr();
        LambdaExpr lambdaExpr = ifPresentCall.getArgument(0).asLambdaExpr();

        ExpressionResolutionResult findScopeResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                findCall.getScope().get(),
                context
        );
        if (findScopeResult.isEmpty()) {
            return findScopeResult;
        }

        Set<RawNode> elementNodes = new LinkedHashSet<>();
        for (RawNode scopeNode : findScopeResult.getNodes()) {
            RawNode elementNode = rawTree.addChild(
                    scopeNode,
                    null,
                    FlowKind.COLLECTION_ELEMENT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            elementNodes.add(elementNode);
        }

        AnalysisStep lambdaStep = lambdaSupport.createLambdaStep(
                step,
                lambdaExpr,
                elementNodes,
                findScopeResult.isUncertain()
        );
        ExpressionResolutionResult bodyResult = lambdaSupport.resolveLambdaBody(
                rawTree,
                lambdaStep,
                lambdaExpr,
                context
        );
        StreamCollectorSupport.markTerminal(bodyResult);

        return new ExpressionResolutionResult(
                bodyResult.getNodes(),
                findScopeResult.isUncertain() || bodyResult.isUncertain()
        );
    }
}