package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_CollectionGetExpressionHandler")
@Order(150)
public class CollectionGetExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return "get".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && !methodCallExpr.getArguments().isEmpty();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

        ExpressionResolutionResult scopeResult = context.getExpressionResolver().resolveAll(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );

        if (scopeResult.isEmpty()) {
            return scopeResult.isUncertain()
                    ? ExpressionResolutionResult.uncertainEmpty()
                    : ExpressionResolutionResult.empty();
        }

        Set<RawNode> resultNodes = new LinkedHashSet<>();

        for (RawNode scopeNode : scopeResult.getNodes()) {
            RawNode elementNode = rawTree.addChild(
                    scopeNode,
                    null,
                    FlowKind.COLLECTION_ELEMENT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            resultNodes.add(elementNode);
        }

        return new ExpressionResolutionResult(resultNodes, scopeResult.isUncertain());
    }
}
