package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

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
    public RawNode resolve(RawTree rawTree,
                           AnalysisStep step,
                           Expression expression,
                           EngineContext context) {
        MethodCallExpr expr = expression.asMethodCallExpr();

        if (expr.getScope().isEmpty()) {
            return null;
        }

        RawNode baseNode = context.getExpressionResolver().resolve(
                rawTree,
                step,
                expr.getScope().get(),
                context
        );

        if (baseNode == null) {
            return null;
        }

        String fieldName = tryResolveGetterName(expr);
        if (fieldName == null) {
            return null;
        }

        return rawTree.addChild(
                baseNode,
                fieldName,
                FlowKind.DIRECT,
                null,
                UsageKind.INTERMEDIATE
        );
    }

    private String tryResolveGetterName(MethodCallExpr expr) {
        String methodName = expr.getNameAsString();

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
