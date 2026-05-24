package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_StreamSortedComparatorExpressionHandler")
@Order(163)
public class StreamSortedComparatorExpressionHandler implements ExpressionHandler {

    private final LambdaElementBindingSupport lambdaSupport = new LambdaElementBindingSupport();
    private final StreamComparatorSupport comparatorSupport;

    public StreamSortedComparatorExpressionHandler() {
        this(new GetterPropertyAccessResolver());
    }

    @Autowired
    public StreamSortedComparatorExpressionHandler(SourceAnalysisCache sourceAnalysisCache) {
        this(new GetterPropertyAccessResolver(sourceAnalysisCache));
    }

    StreamSortedComparatorExpressionHandler(GetterPropertyAccessResolver getterPropertyAccessResolver) {
        this.comparatorSupport = new StreamComparatorSupport(getterPropertyAccessResolver);
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
                && comparatorSupport.resolveComparingCall(sortedCall.getArgument(0)).isPresent();
    }

    @Override
    public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                 AnalysisStep step,
                                                 Expression expression,
                                                 EngineContext context) {
        MethodCallExpr sortedCall = expression.asMethodCallExpr();
        MethodCallExpr comparingCall = comparatorSupport.resolveComparingCall(sortedCall.getArgument(0)).orElseThrow();

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

        ExpressionResolutionResult keyResult = comparatorSupport.resolveKeyExtractor(
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
}