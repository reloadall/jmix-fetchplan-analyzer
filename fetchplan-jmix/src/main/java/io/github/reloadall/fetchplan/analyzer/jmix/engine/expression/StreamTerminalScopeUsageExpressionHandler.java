package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_StreamTerminalScopeUsageExpressionHandler")
@Order(170)
public class StreamTerminalScopeUsageExpressionHandler implements ExpressionHandler {

    private static final Set<String> DIRECT_TERMINAL_METHODS = Set.of(
            "toList",
            "count"
    );

    private static final Set<String> SIMPLE_COLLECTOR_METHODS = Set.of(
            "toList",
            "toSet",
            "toUnmodifiableList",
            "toUnmodifiableSet",
            "toCollection"
    );

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        return supportsDirectTerminal(methodCallExpr) || supportsSimpleCollector(methodCallExpr);
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
        StreamCollectorSupport.markTerminal(scopeResult);
        return scopeResult;
    }

    private boolean supportsDirectTerminal(MethodCallExpr methodCallExpr) {
        return DIRECT_TERMINAL_METHODS.contains(methodCallExpr.getNameAsString())
                && methodCallExpr.getScope().isPresent()
                && methodCallExpr.getArguments().isEmpty();
    }

    private boolean supportsSimpleCollector(MethodCallExpr methodCallExpr) {
        if (!"collect".equals(methodCallExpr.getNameAsString())
                || methodCallExpr.getScope().isEmpty()
                || methodCallExpr.getArguments().size() != 1
                || !methodCallExpr.getArgument(0).isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr collectorCall = methodCallExpr.getArgument(0).asMethodCallExpr();
        return SIMPLE_COLLECTOR_METHODS.contains(collectorCall.getNameAsString())
                && isSupportedCollectorArgumentCount(collectorCall)
                && StreamCollectorSupport.isSupportedCollectorsScope(collectorCall);
    }

    private boolean isSupportedCollectorArgumentCount(MethodCallExpr collectorCall) {
        if ("toCollection".equals(collectorCall.getNameAsString())) {
            return collectorCall.getArguments().size() == 1;
        }

        return collectorCall.getArguments().isEmpty();
    }

}