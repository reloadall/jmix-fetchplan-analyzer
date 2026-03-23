package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_MethodCallExpressionHandler")
@Order(200)
public class MethodCallExpressionHandler implements ExpressionHandler {

    @Override
    public boolean supports(Expression expression) {
        return expression.isMethodCallExpr();
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

        String fieldName = extractFieldName(methodCallExpr);
        if (fieldName == null) {
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
            RawNode child = rawTree.addChild(
                    scopeNode,
                    fieldName,
                    FlowKind.DIRECT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            resultNodes.add(child);
        }

        return new ExpressionResolutionResult(resultNodes, scopeResult.isUncertain());
    }

    private String extractFieldName(MethodCallExpr methodCallExpr) {
        String methodName = methodCallExpr.getNameAsString();

        if (methodName.startsWith("get") && methodName.length() > 3 && methodCallExpr.getArguments().isEmpty()) {
            return decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2 && methodCallExpr.getArguments().isEmpty()) {
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
