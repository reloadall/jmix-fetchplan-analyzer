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

@Component("fpa_StreamCollectToMapExpressionHandler")
@Order(168)
public class StreamCollectToMapExpressionHandler implements ExpressionHandler {

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();
    private final GetterPropertyAccessResolver getterPropertyAccessResolver;

    public StreamCollectToMapExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public StreamCollectToMapExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    StreamCollectToMapExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
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
        int mapperCount = collectorCall.getArguments().size();
        return "toMap".equals(collectorCall.getNameAsString())
                && mapperCount >= 2
                && mapperCount <= 4
                && isSupportedCollectorsScope(collectorCall);
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

        ExpressionResolutionResult keyResult = resolveMapper(
                rawTree,
                step,
                collectorCall.getArgument(0),
                scopeElements,
                context
        );
        ExpressionResolutionResult valueResult = resolveMapper(
                rawTree,
                step,
                collectorCall.getArgument(1),
                scopeElements,
                context
        );

        markTerminal(keyResult);
        markTerminal(valueResult);

        return keyResult.merge(valueResult);
    }

    private ExpressionResolutionResult resolveMapper(RawTree rawTree,
                                                    AnalysisStep step,
                                                    Expression mapper,
                                                    LambdaElementBindingSupport.ScopeElements scopeElements,
                                                    EngineContext context) {
        if (mapper.isLambdaExpr()) {
            return resolveLambdaMapper(rawTree, step, mapper.asLambdaExpr(), scopeElements, context);
        }

        if (mapper.isMethodReferenceExpr()) {
            return resolveMethodReferenceMapper(rawTree, step, mapper.asMethodReferenceExpr(), scopeElements);
        }

        if (isFunctionIdentity(mapper)) {
            return scopeElements.scopeResult();
        }

        return ExpressionResolutionResult.empty();
    }

    private ExpressionResolutionResult resolveLambdaMapper(RawTree rawTree,
                                                          AnalysisStep step,
                                                          LambdaExpr lambdaExpr,
                                                          LambdaElementBindingSupport.ScopeElements scopeElements,
                                                          EngineContext context) {
        if (lambdaExpr.getParameters().size() != 1 || !isSupportedLambdaBody(lambdaExpr)) {
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
        markTerminal(lambdaReturnResult.preReturnReads());
        ExpressionResolutionResult returnedResult = lambdaReturnResult.returnedResult();
        return new ExpressionResolutionResult(
                returnedResult.getNodes(),
                scopeElements.scopeResult().isUncertain()
                        || returnedResult.isUncertain()
                        || lambdaReturnResult.preReturnReads().isUncertain()
        );
    }

    private ExpressionResolutionResult resolveMethodReferenceMapper(RawTree rawTree,
                                                                   AnalysisStep step,
                                                                   MethodReferenceExpr methodReferenceExpr,
                                                                   LambdaElementBindingSupport.ScopeElements scopeElements) {
        Optional<String> mappedField = resolveMappedField(step, methodReferenceExpr);
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

    private boolean isSupportedCollectorsScope(MethodCallExpr collectorCall) {
        if (collectorCall.getScope().isEmpty()) {
            return true;
        }

        String scope = collectorCall.getScope().get().toString();
        return "Collectors".equals(scope) || "java.util.stream.Collectors".equals(scope);
    }

    private boolean isFunctionIdentity(Expression mapper) {
        if (!mapper.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr methodCallExpr = mapper.asMethodCallExpr();
        if (!"identity".equals(methodCallExpr.getNameAsString()) || !methodCallExpr.getArguments().isEmpty()) {
            return false;
        }

        return methodCallExpr.getScope()
                .map(scope -> "Function".equals(scope.toString()) || "java.util.function.Function".equals(scope.toString()))
                .orElse(false);
    }

    private boolean isSupportedLambdaBody(LambdaExpr lambdaExpr) {
        if (lambdaExpr.getBody().isExpressionStmt()) {
            return true;
        }

        return lambdaExpr.getBody().isBlockStmt()
                && lambdaExpr.getBody().asBlockStmt().getStatements().stream().anyMatch(statement -> statement.isReturnStmt());
    }

    private Optional<String> resolveMappedField(AnalysisStep step, MethodReferenceExpr methodReferenceExpr) {
        String ownerTypeName = methodReferenceExpr.getScope().toString();
        return getterPropertyAccessResolver.resolveBackedPropertyName(
                step.getMethod(),
                ownerTypeName,
                methodReferenceExpr.getIdentifier()
        );
    }

    private void markTerminal(ExpressionResolutionResult result) {
        for (RawNode node : result.getNodes()) {
            node.setUsageKind(UsageKind.TERMINAL);
        }
    }
}