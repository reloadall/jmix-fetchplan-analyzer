package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

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
        if (!"map".equals(methodCallExpr.getNameAsString())) {
            return false;
        }

        if (methodCallExpr.getScope().isEmpty()) {
            return false;
        }

        if (methodCallExpr.getArguments().size() != 1) {
            return false;
        }

        return methodCallExpr.getArgument(0).isMethodReferenceExpr();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        MethodReferenceExpr methodReferenceExpr = methodCallExpr.getArgument(0).asMethodReferenceExpr();

        String mappedField = extractFieldName(methodReferenceExpr.getIdentifier());
        if (mappedField == null) {
            return ExpressionResolutionResult.empty();
        }

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

            RawNode mappedNode = rawTree.addChild(
                    elementNode,
                    mappedField,
                    FlowKind.DIRECT,
                    null,
                    UsageKind.INTERMEDIATE
            );

            resultNodes.add(mappedNode);
        }

        return new ExpressionResolutionResult(resultNodes, scopeResult.isUncertain());
    }

    private String extractFieldName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }

        return null;
    }

    private String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.length() == 1) {
            return value.toLowerCase();
        }

        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
