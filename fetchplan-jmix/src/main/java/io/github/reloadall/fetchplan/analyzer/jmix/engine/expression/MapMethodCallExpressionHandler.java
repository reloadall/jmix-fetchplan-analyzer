package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_MapMethodCallExpressionHandler")
@Order(160)
public class MapMethodCallExpressionHandler implements ExpressionHandler {

    private final GetterPropertyAccessResolver getterPropertyAccessResolver;

    public MapMethodCallExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public MapMethodCallExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    MapMethodCallExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.getterPropertyAccessResolver = getterPropertyAccessResolver;
    }

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

        Optional<String> mappedField = resolveMappedField(step, methodReferenceExpr);
        if (mappedField.isEmpty()) {
            return scopeResult;
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
                    mappedField.get(),
                    FlowKind.DIRECT,
                    null,
                    UsageKind.INTERMEDIATE
            );

            resultNodes.add(mappedNode);
        }

        return new ExpressionResolutionResult(resultNodes, scopeResult.isUncertain());
    }

    private Optional<String> resolveMappedField(AnalysisStep step, MethodReferenceExpr methodReferenceExpr) {
        String ownerTypeName = methodReferenceExpr.getScope().toString();
        return getterPropertyAccessResolver.resolveBackedPropertyName(
                step.getMethod(),
                ownerTypeName,
                methodReferenceExpr.getIdentifier()
        );
    }
}
