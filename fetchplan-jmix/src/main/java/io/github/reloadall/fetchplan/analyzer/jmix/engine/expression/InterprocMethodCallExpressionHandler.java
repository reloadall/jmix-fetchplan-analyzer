package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocArgumentBinder;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocReturnResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocMethodCallExpressionHandler")
@Order(185)
public class InterprocMethodCallExpressionHandler implements ExpressionHandler {

    private final InterprocMethodResolver interprocMethodResolver;
    private final InterprocArgumentBinder interprocArgumentBinder;
    private final InterprocReturnResolver interprocReturnResolver;
    private final AnalysisTrace analysisTrace;

    private final ThreadLocal<Set<String>> activeCalls =
            ThreadLocal.withInitial(LinkedHashSet::new);

    @Autowired
    public InterprocMethodCallExpressionHandler(InterprocMethodResolver interprocMethodResolver,
                                                InterprocArgumentBinder interprocArgumentBinder,
                                                InterprocReturnResolver interprocReturnResolver,
                                                AnalysisTrace analysisTrace) {
        this.interprocMethodResolver = Objects.requireNonNull(interprocMethodResolver, "interprocMethodResolver is null");
        this.interprocArgumentBinder = Objects.requireNonNull(interprocArgumentBinder, "interprocArgumentBinder is null");
        this.interprocReturnResolver = Objects.requireNonNull(interprocReturnResolver, "interprocReturnResolver is null");
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
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

        Optional<MethodDeclaration> targetMethodOpt = interprocMethodResolver.resolve(methodCallExpr, step);
        if (targetMethodOpt.isEmpty()) {
            return ExpressionResolutionResult.empty();
        }

        MethodDeclaration targetMethod = targetMethodOpt.get();
        if (targetMethod.getBody().isEmpty()) {
            analysisTrace.log("INTERPROC-EXPR: skip, target method has no body: " + methodCallExpr);
            return ExpressionResolutionResult.empty();
        }

        Optional<Map<String, ValueBinding>> targetBindingsOpt = interprocArgumentBinder.bindArguments(
                rawTree,
                step,
                methodCallExpr,
                targetMethod,
                context
        );
        if (targetBindingsOpt.isEmpty()) {
            analysisTrace.log("INTERPROC-EXPR: skip, no meaningful bindings: " + methodCallExpr);
            return ExpressionResolutionResult.empty();
        }

        Map<String, ValueBinding> targetBindings = targetBindingsOpt.get();
        RawNode entryAnchor = resolveEntryAnchor(targetBindings, step.getCurrentRawNode());

        String recursionKey = buildRecursionKey(targetMethod, entryAnchor, targetBindings);
        Set<String> active = activeCalls.get();
        if (!active.add(recursionKey)) {
            analysisTrace.log("INTERPROC-EXPR: recursion guard hit for " + recursionKey);
            return ExpressionResolutionResult.empty();
        }

        try {
            ExpressionResolutionResult result = interprocReturnResolver.resolveReturnValue(
                    rawTree,
                    targetMethod,
                    targetBindings,
                    entryAnchor,
                    context
            );

            if (!result.isEmpty()) {
                analysisTrace.log("INTERPROC-EXPR: resolved " + methodCallExpr
                        + " -> nodes="
                        + result.getNodes().stream().map(node -> String.valueOf(node.getId())).toList());
            } else {
                analysisTrace.log("INTERPROC-EXPR: unresolved return node for " + methodCallExpr);
            }

            return result;
        } finally {
            active.remove(recursionKey);
        }
    }

    private RawNode resolveEntryAnchor(Map<String, ValueBinding> targetBindings, RawNode fallback) {
        long nonEmptyBindings = targetBindings.values().stream()
                .filter(binding -> binding != null && !binding.isEmpty() && !binding.isTerminalOnly())
                .count();

        if (nonEmptyBindings > 1) {
            return fallback;
        }

        for (ValueBinding binding : targetBindings.values()) {
            if (!binding.isEmpty() && !binding.isTerminalOnly()) {
                return binding.getNodes().iterator().next();
            }
        }
        return fallback;
    }

    private String buildRecursionKey(MethodDeclaration targetMethod,
                                     RawNode entryAnchor,
                                     Map<String, ValueBinding> targetBindings) {
        String bindingsKey = new TreeSet<>(targetBindings.keySet()).stream()
                .map(key -> key + "=" + targetBindings.get(key).getNodes().stream()
                        .map(RawNode::getId)
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("|"));

        return targetMethod.getDeclarationAsString(false, false, false)
                + "@anchor=" + entryAnchor.getId()
                + "@bindings=" + bindingsKey;
    }
}
