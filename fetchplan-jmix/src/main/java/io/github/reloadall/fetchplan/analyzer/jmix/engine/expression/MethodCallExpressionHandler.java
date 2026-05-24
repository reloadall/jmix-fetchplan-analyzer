package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_MethodCallExpressionHandler")
@Order(200)
public class MethodCallExpressionHandler implements ExpressionHandler {

    private final GetterPropertyAccessResolver getterPropertyAccessResolver;

    public MethodCallExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public MethodCallExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    MethodCallExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.getterPropertyAccessResolver = getterPropertyAccessResolver;
    }

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

        Set<String> fieldNames = getterPropertyAccessResolver.resolvePropertyNames(methodCallExpr, step.getMethod());
        if (fieldNames.isEmpty()) {
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
            for (String fieldName : fieldNames) {
                RawNode child = rawTree.addChild(
                        scopeNode,
                        fieldName,
                        FlowKind.DIRECT,
                        null,
                        UsageKind.INTERMEDIATE
                );
                resultNodes.add(child);
            }
        }

        return new ExpressionResolutionResult(resultNodes, scopeResult.isUncertain());
    }
}
