package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

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
        return isCollectionGet(methodCallExpr);
    }

    @Override
    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

        if (methodCallExpr.getScope().isEmpty()) {
            return null;
        }

        RawNode collectionNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );

        if (collectionNode == null) {
            return null;
        }

        return rawTree.addChild(
                collectionNode,
                null,
                FlowKind.COLLECTION_ELEMENT,
                null,
                UsageKind.INTERMEDIATE
        );
    }

    private boolean isCollectionGet(MethodCallExpr methodCallExpr) {
        return "get".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getArguments().size() == 1
                && methodCallExpr.getScope().isPresent();
    }
}
