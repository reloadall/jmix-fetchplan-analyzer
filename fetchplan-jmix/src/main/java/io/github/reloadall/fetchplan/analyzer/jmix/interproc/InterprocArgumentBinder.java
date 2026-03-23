package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocArgumentBinder")
public class InterprocArgumentBinder {

    private final AnalysisTrace analysisTrace;

    public InterprocArgumentBinder(AnalysisTrace analysisTrace) {
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public Optional<Map<String, ValueBinding>> bindArguments(RawTree rawTree,
                                                             AnalysisStep step,
                                                             MethodCallExpr methodCallExpr,
                                                             MethodDeclaration targetMethod,
                                                             EngineContext context) {
        if (methodCallExpr.getArguments().size() != targetMethod.getParameters().size()) {
            analysisTrace.log("INTERPROC: bindArguments failed, arity mismatch: args="
                    + methodCallExpr.getArguments().size()
                    + ", params=" + targetMethod.getParameters().size());
            return Optional.empty();
        }

        Map<String, ValueBinding> bindings = new LinkedHashMap<>();

        for (int i = 0; i < methodCallExpr.getArguments().size(); i++) {
            Parameter parameter = targetMethod.getParameter(i);

            ExpressionResolutionResult result = context.getExpressionResolver().resolveAll(
                    rawTree,
                    step,
                    methodCallExpr.getArgument(i),
                    context
            );

            if (!result.isEmpty()) {
                bindings.put(parameter.getNameAsString(), ValueBinding.from(result));
                analysisTrace.log("INTERPROC: arg[" + i + "] param=" + parameter.getNameAsString()
                        + " resolved to nodes="
                        + result.getNodes().stream().map(n -> String.valueOf(n.getId())).toList());
            } else {
                analysisTrace.log("INTERPROC: arg[" + i + "] param=" + parameter.getNameAsString()
                        + " unresolved, skipped");
            }
        }

        if (bindings.isEmpty()) {
            analysisTrace.log("INTERPROC: bindArguments failed, no path-relevant arguments resolved");
            return Optional.empty();
        }

        analysisTrace.log("INTERPROC: bindArguments success, bound params = " + bindings.keySet());
        return Optional.of(bindings);
    }
}
