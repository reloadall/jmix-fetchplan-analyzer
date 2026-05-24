package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
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

@Component("fpa_StreamCollectGroupingByExpressionHandler")
@Order(169)
public class StreamCollectGroupingByExpressionHandler implements ExpressionHandler {

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();
    private final GetterPropertyAccessResolver getterPropertyAccessResolver;

    public StreamCollectGroupingByExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public StreamCollectGroupingByExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    StreamCollectGroupingByExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.getterPropertyAccessResolver = getterPropertyAccessResolver;
    }

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr collectCall = expression.asMethodCallExpr();
        if (!"collect".equals(collectCall.getNameAsString())
                || collectCall.getScope().isEmpty()
                || collectCall.getArguments().size() != 1
                || !collectCall.getArgument(0).isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr collectorCall = collectCall.getArgument(0).asMethodCallExpr();
        int argumentCount = collectorCall.getArguments().size();
        return "groupingBy".equals(collectorCall.getNameAsString())
                && argumentCount >= 1
                && argumentCount <= 3
                && StreamCollectorSupport.isSupportedCollectorsScope(collectorCall);
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr collectCall = expression.asMethodCallExpr();
        MethodCallExpr collectorCall = collectCall.getArgument(0).asMethodCallExpr();

        LambdaElementBindingSupport.ScopeElements scopeElements = lambdaSupport.resolveScopeElements(
                rawTree,
                step,
                collectCall,
                context
        );
        ExpressionResolutionResult scopeResult = scopeElements.scopeResult();
        if (scopeResult.isEmpty()) {
            return scopeResult;
        }

        ExpressionResolutionResult classifierResult = resolveClassifier(
                rawTree,
                step,
                collectorCall.getArgument(0),
                scopeElements,
                context
        );
        StreamCollectorSupport.markTerminal(classifierResult);
        return classifierResult;
    }

    private ExpressionResolutionResult resolveClassifier(RawTree rawTree,
                                                        AnalysisStep step,
                                                        Expression classifier,
                                                        LambdaElementBindingSupport.ScopeElements scopeElements,
                                                        EngineContext context) {
        if (classifier.isLambdaExpr()) {
            return resolveLambdaClassifier(rawTree, step, classifier.asLambdaExpr(), scopeElements, context);
        }

        if (classifier.isMethodReferenceExpr()) {
            return resolveMethodReferenceClassifier(rawTree, step, classifier.asMethodReferenceExpr(), scopeElements);
        }

        return ExpressionResolutionResult.empty();
    }

    private ExpressionResolutionResult resolveLambdaClassifier(RawTree rawTree,
                                                              AnalysisStep step,
                                                              LambdaExpr lambdaExpr,
                                                              LambdaElementBindingSupport.ScopeElements scopeElements,
                                                              EngineContext context) {
        if (lambdaExpr.getParameters().size() != 1
                || !StreamCollectorSupport.isSupportedReturnLikeLambdaBody(lambdaExpr)) {
            return ExpressionResolutionResult.empty();
        }

        AnalysisStep lambdaStep = lambdaSupport.createLambdaStep(
                step,
                lambdaExpr,
                scopeElements.elementNodes(),
                scopeElements.scopeResult().isUncertain()
        );
        LambdaElementBindingSupport.LambdaReturnResult lambdaReturnResult = lambdaSupport.resolveLambdaReturnBody(
                rawTree,
                lambdaStep,
                lambdaExpr,
                context
        );
        StreamCollectorSupport.markTerminal(lambdaReturnResult.preReturnReads());
        ExpressionResolutionResult returnedResult = lambdaReturnResult.returnedResult();
        return new ExpressionResolutionResult(
                returnedResult.getNodes(),
                scopeElements.scopeResult().isUncertain()
                        || returnedResult.isUncertain()
                        || lambdaReturnResult.preReturnReads().isUncertain()
        );
    }

    private ExpressionResolutionResult resolveMethodReferenceClassifier(RawTree rawTree,
                                                                       AnalysisStep step,
                                                                       MethodReferenceExpr methodReferenceExpr,
                                                                       LambdaElementBindingSupport.ScopeElements scopeElements) {
        Optional<String> mappedField = StreamCollectorSupport.resolveBackedMethodReferenceField(
                step,
                methodReferenceExpr,
                getterPropertyAccessResolver
        );
        if (mappedField.isEmpty()) {
            return ExpressionResolutionResult.empty();
        }

        Set<RawNode> resultNodes = new LinkedHashSet<>();
        for (RawNode elementNode : scopeElements.elementNodes()) {
            RawNode mappedNode = rawTree.addChild(
                    elementNode,
                    mappedField.get(),
                    FlowKind.DIRECT,
                    null,
                    UsageKind.INTERMEDIATE
            );
            resultNodes.add(mappedNode);
        }

        return new ExpressionResolutionResult(resultNodes, scopeElements.scopeResult().isUncertain());
    }

}