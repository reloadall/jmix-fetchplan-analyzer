package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_StreamMinMaxComparatorExpressionHandler")
@Order(164)
public class StreamMinMaxComparatorExpressionHandler implements ExpressionHandler {

    private static final Set<String> SUPPORTED_METHODS = Set.of("min", "max");

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();
    private final StreamComparatorSupport comparatorSupport;

    public StreamMinMaxComparatorExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public StreamMinMaxComparatorExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    StreamMinMaxComparatorExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.comparatorSupport = new StreamComparatorSupport(getterPropertyAccessResolver);
    }

    @Override
    public boolean supports(Expression expression) {
        if (!expression.isMethodCallExpr()) {
            return false;
        }

        MethodCallExpr terminalCall = expression.asMethodCallExpr();
        return SUPPORTED_METHODS.contains(terminalCall.getNameAsString())
                && terminalCall.getScope().isPresent()
                && terminalCall.getArguments().size() == 1
                && comparatorSupport.resolveComparingCall(terminalCall.getArgument(0)).isPresent();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr terminalCall = expression.asMethodCallExpr();
        MethodCallExpr comparingCall = comparatorSupport.resolveComparingCall(terminalCall.getArgument(0)).orElseThrow();

        LambdaElementBindingSupport.ScopeElements scopeElements = lambdaSupport.resolveScopeElements(
                rawTree,
                step,
                terminalCall,
                context
        );
        ExpressionResolutionResult scopeResult = scopeElements.scopeResult();
        if (scopeResult.isEmpty()) {
            return scopeResult;
        }

        ExpressionResolutionResult keyResult = comparatorSupport.resolveKeyExtractor(
                rawTree,
                step,
                comparingCall.getArgument(0),
                scopeElements,
                context
        );

        StreamCollectorSupport.markTerminal(keyResult);
        StreamCollectorSupport.markTerminal(scopeResult);

        Set<RawNode> resultNodes = new LinkedHashSet<>(scopeResult.getNodes());
        resultNodes.addAll(keyResult.getNodes());
        return new ExpressionResolutionResult(
                resultNodes,
                scopeResult.isUncertain() || keyResult.isUncertain()
        );
    }
}