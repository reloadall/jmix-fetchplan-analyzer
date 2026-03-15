package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocCallPlanner")
public class InterprocCallPlanner {

    private final InterprocMethodResolver interprocMethodResolver;
    private final InterprocArgumentBinder interprocArgumentBinder;
    private final InterprocReturnResolver interprocReturnResolver;

    @Autowired
    public InterprocCallPlanner(InterprocMethodResolver interprocMethodResolver,
                                InterprocArgumentBinder interprocArgumentBinder,
                                InterprocReturnResolver interprocReturnResolver) {
        this.interprocMethodResolver = interprocMethodResolver;
        this.interprocArgumentBinder = interprocArgumentBinder;
        this.interprocReturnResolver = interprocReturnResolver;
    }

    public Optional<InterprocCallPlan> plan(RawTree rawTree,
                                            AnalysisStep step,
                                            MethodCallExpr methodCallExpr,
                                            EngineContext context) {
        if (!(step.getPayload() instanceof StatementsPayload currentPayload)) {
            return Optional.empty();
        }

        Optional<TargetInvocation> targetInvocationOpt = resolveTargetInvocation(
                rawTree,
                step,
                methodCallExpr,
                context
        );
        if (targetInvocationOpt.isEmpty()) {
            return Optional.empty();
        }

        TargetInvocation targetInvocation = targetInvocationOpt.get();

        StatementsPayload afterCallPayload = currentPayload.next();
        Continuation returnToCaller = new Continuation(
                step.getMethod(),
                afterCallPayload,
                step.getCurrentRawNode(),
                step.getBindings()
        );

        StatementsPayload targetPayload = StatementsPayload.from(targetInvocation.targetMethod)
                .withContinuationOnFinish(returnToCaller);

        Continuation targetMethodContinuation = new Continuation(
                targetInvocation.targetMethod,
                targetPayload,
                targetInvocation.entryAnchor,
                targetInvocation.targetBindings
        );

        return Optional.of(new InterprocCallPlan(targetMethodContinuation));
    }

    public Optional<InterprocCallPlan> planValueCall(RawTree rawTree,
                                                     AnalysisStep step,
                                                     MethodCallExpr methodCallExpr,
                                                     String targetVariableName,
                                                     EngineContext context) {
        if (!(step.getPayload() instanceof StatementsPayload currentPayload)) {
            return Optional.empty();
        }

        Optional<TargetInvocation> targetInvocationOpt = resolveTargetInvocation(
                rawTree,
                step,
                methodCallExpr,
                context
        );
        if (targetInvocationOpt.isEmpty()) {
            return Optional.empty();
        }

        TargetInvocation targetInvocation = targetInvocationOpt.get();

        Optional<RawNode> returnNodeOpt = interprocReturnResolver.resolveReturnNode(
                rawTree,
                targetInvocation.targetMethod,
                targetInvocation.targetBindings,
                targetInvocation.entryAnchor,
                context
        );
        if (returnNodeOpt.isEmpty()) {
            return Optional.empty();
        }

        RawNode boundReturnNode = bindReturnValue(rawTree, targetVariableName, returnNodeOpt.get());
        boundReturnNode.setUsageKind(UsageKind.INTERMEDIATE);

        Map<String, RawNode> callerBindings = new HashMap<>(step.getBindings());
        callerBindings.put(targetVariableName, boundReturnNode);

        StatementsPayload afterCallPayload = currentPayload.next();
        Continuation returnToCaller = new Continuation(
                step.getMethod(),
                afterCallPayload,
                step.getCurrentRawNode(),
                callerBindings
        );

        StatementsPayload targetPayload = StatementsPayload.from(targetInvocation.targetMethod)
                .withContinuationOnFinish(returnToCaller);

        Continuation targetMethodContinuation = new Continuation(
                targetInvocation.targetMethod,
                targetPayload,
                targetInvocation.entryAnchor,
                targetInvocation.targetBindings
        );

        return Optional.of(new InterprocCallPlan(targetMethodContinuation));
    }

    private Optional<TargetInvocation> resolveTargetInvocation(RawTree rawTree,
                                                               AnalysisStep step,
                                                               MethodCallExpr methodCallExpr,
                                                               EngineContext context) {
        Optional<MethodDeclaration> targetMethodOpt = interprocMethodResolver.resolve(methodCallExpr, step);
        if (targetMethodOpt.isEmpty()) {
            return Optional.empty();
        }

        MethodDeclaration targetMethod = targetMethodOpt.get();
        if (targetMethod.getBody().isEmpty()) {
            return Optional.empty();
        }

        Optional<Map<String, RawNode>> targetBindingsOpt = interprocArgumentBinder.bindArguments(
                rawTree,
                step,
                methodCallExpr,
                targetMethod,
                context
        );
        if (targetBindingsOpt.isEmpty()) {
            return Optional.empty();
        }

        Map<String, RawNode> targetBindings = targetBindingsOpt.get();
        if (targetBindings.isEmpty()) {
            return Optional.empty();
        }

        RawNode entryAnchor = resolveEntryAnchor(targetBindings, step.getCurrentRawNode());

        return Optional.of(new TargetInvocation(targetMethod, targetBindings, entryAnchor));
    }

    private RawNode bindReturnValue(RawTree rawTree, String variableName, RawNode returnNode) {
        if (shouldCreateAlias(variableName, returnNode)) {
            return rawTree.addAlias(returnNode, variableName);
        }
        return returnNode;
    }

    private boolean shouldCreateAlias(String variableName, RawNode resolvedNode) {
        if (variableName == null || variableName.isBlank()) {
            return false;
        }

        if (variableName.equals(resolvedNode.getVariableName())) {
            return false;
        }

        if (variableName.equals(resolvedNode.getEntityField())) {
            return false;
        }

        return true;
    }

    private RawNode resolveEntryAnchor(Map<String, RawNode> targetBindings, RawNode fallback) {
        return targetBindings.values().stream()
                .findFirst()
                .orElse(fallback);
    }

    private static class TargetInvocation {
        private final MethodDeclaration targetMethod;
        private final Map<String, RawNode> targetBindings;
        private final RawNode entryAnchor;

        private TargetInvocation(MethodDeclaration targetMethod,
                                 Map<String, RawNode> targetBindings,
                                 RawNode entryAnchor) {
            this.targetMethod = targetMethod;
            this.targetBindings = targetBindings;
            this.entryAnchor = entryAnchor;
        }
    }
}
