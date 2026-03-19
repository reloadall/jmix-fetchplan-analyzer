package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocArgumentBinder")
public class InterprocArgumentBinder {

    public Optional<Map<String, RawNode>> bindArguments(RawTree rawTree,
                                                        AnalysisStep step,
                                                        MethodCallExpr methodCallExpr,
                                                        MethodDeclaration targetMethod,
                                                        EngineContext context) {
        if (methodCallExpr.getArguments().size() != targetMethod.getParameters().size()) {
            return Optional.empty();
        }

        Map<String, RawNode> bindings = new LinkedHashMap<>();

        for (int i = 0; i < methodCallExpr.getArguments().size(); i++) {
            Expression argument = methodCallExpr.getArgument(i);
            Parameter parameter = targetMethod.getParameter(i);

            RawNode argumentNode = context.getExpressionResolver().resolve(
                    rawTree,
                    step,
                    argument,
                    context
            );

            if (argumentNode != null) {
                bindings.put(parameter.getNameAsString(), argumentNode);
            }
        }

        if (bindings.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(bindings);
    }
}
