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

@Component("fpa_StreamSortedComparatorExpressionHandler")
@Order(163)
public class StreamSortedComparatorExpressionHandler implements ExpressionHandler {

    private static final Set<String> SUPPORTED_COMPARING_METHODS = Set.of(
            "comparing",
            "comparingInt",
            "comparingLong",
            "comparingDouble"
    );

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();
    private final GetterPropertyAccessResolver getterPropertyAccessResolver;

    public StreamSortedComparatorExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public StreamSortedComparatorExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    StreamSortedComparatorExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.getterPropertyAccessResolver = getterPropertyAccessResolver;
    }

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr sortedCall = expression.asMethodCallExpr();
        return "sorted".equals(sortedCall.getNameAsString())
                && sortedCall.getScope().isPresent()
                && sortedCall.getArguments().size() == 1
                && resolveComparingCall(sortedCall.getArgument(0)).isPresent();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr sortedCall = expression.asMethodCallExpr();
        MethodCallExpr comparingCall = resolveComparingCall(sortedCall.getArgument(0)).orElseThrow();

        LambdaElementBindingSupport.ScopeElements scopeElements = lambdaSupport.resolveScopeElements(
                rawTree,
                step,
                sortedCall,
                context
        );
        ExpressionResolutionResult scopeResult = scopeElements.scopeResult();
        if (scopeResult.isEmpty()) {
            return scopeResult;
        }

        ExpressionResolutionResult keyResult = resolveKeyExtractor(
                rawTree,
                step,
                comparingCall.getArgument(0),
                scopeElements,
                context
        );
        StreamCollectorSupport.markTerminal(keyResult);

        return new ExpressionResolutionResult(
                scopeResult.getNodes(),
                scopeResult.isUncertain() || keyResult.isUncertain()
        );
    }

    private ExpressionResolutionResult resolveKeyExtractor(RawTree rawTree,
                                                          AnalysisStep step,
                                                          Expression extractor,
                                                          LambdaElementBindingSupport.ScopeElements scopeElements,
                                                          EngineContext context) {
        if (extractor.isLambdaExpr()) {
            return resolveLambdaExtractor(rawTree, step, extractor.asLambdaExpr(), scopeElements, context);
        }

        if (extractor.isMethodReferenceExpr()) {
            return resolveMethodReferenceExtractor(rawTree, step, extractor.asMethodReferenceExpr(), scopeElements);
        }

        return ExpressionResolutionResult.empty();
    }

    private ExpressionResolutionResult resolveLambdaExtractor(RawTree rawTree,
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

    private ExpressionResolutionResult resolveMethodReferenceExtractor(RawTree rawTree,
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

    private Optional<MethodCallExpr> resolveComparingCall(Expression comparatorExpression) {
        if (!comparatorExpression.isMethodCallExpr()) {
            return Optional.empty();
        }

        MethodCallExpr comparatorCall = comparatorExpression.asMethodCallExpr();
        if (isReversedComparatorCall(comparatorCall)) {
            return comparatorCall.getScope()
                    .filter(Expression::isMethodCallExpr)
                    .map(Expression::asMethodCallExpr)
                    .filter(this::isSupportedComparingCall);
        }

        if (isSupportedComparingCall(comparatorCall)) {
            return Optional.of(comparatorCall);
        }

        return Optional.empty();
    }

    private boolean isReversedComparatorCall(MethodCallExpr comparatorCall) {
        return "reversed".equals(comparatorCall.getNameAsString())
                && comparatorCall.getScope().isPresent()
                && comparatorCall.getArguments().isEmpty();
    }

    private boolean isSupportedComparingCall(MethodCallExpr comparingCall) {
        return SUPPORTED_COMPARING_METHODS.contains(comparingCall.getNameAsString())
                && comparingCall.getArguments().size() == 1
                && comparingCall.getScope()
                .map(scope -> "Comparator".equals(scope.toString()) || "java.util.Comparator".equals(scope.toString()))
                .orElse(false);
    }
}