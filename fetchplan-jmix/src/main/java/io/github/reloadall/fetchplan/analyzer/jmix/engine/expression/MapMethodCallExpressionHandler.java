package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_MapMethodCallExpressionHandler")
@Order(160)
public class MapMethodCallExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return "map".equals(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && methodCallExpr.getArguments().size() == 1
                && methodCallExpr.getArgument(0).isMethodReferenceExpr();
    }

    @Override
    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();

        RawNode scopeNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                methodCallExpr.getScope().get(),
                context
        );
        if (scopeNode == null) {
            return null;
        }

        MethodReferenceExpr methodReferenceExpr = methodCallExpr.getArgument(0).asMethodReferenceExpr();
        String fieldName = tryResolveGetterFieldName(methodReferenceExpr);
        if (fieldName == null) {
            return null;
        }

        RawNode elementNode = rawTree.addChild(
                scopeNode,
                null,
                FlowKind.COLLECTION_ELEMENT,
                null,
                UsageKind.INTERMEDIATE
        );

        return rawTree.addChild(
                elementNode,
                fieldName,
                FlowKind.DIRECT,
                null,
                UsageKind.INTERMEDIATE
        );
    }

    private String tryResolveGetterFieldName(MethodReferenceExpr methodReferenceExpr) {
        String methodName = methodReferenceExpr.getIdentifier();

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }

        return null;
    }

    private String decapitalize(String value) {
        if (value.length() == 1) {
            return value.toLowerCase();
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
