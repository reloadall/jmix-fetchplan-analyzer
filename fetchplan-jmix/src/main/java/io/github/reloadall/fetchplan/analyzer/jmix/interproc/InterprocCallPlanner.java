package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
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
    private final AnalysisTrace analysisTrace;

    @Autowired
    public InterprocCallPlanner(InterprocMethodResolver interprocMethodResolver,
                                InterprocArgumentBinder interprocArgumentBinder,
                                InterprocReturnResolver interprocReturnResolver,
                                AnalysisTrace analysisTrace) {
        this.interprocMethodResolver = interprocMethodResolver;
        this.interprocArgumentBinder = interprocArgumentBinder;
        this.interprocReturnResolver = interprocReturnResolver;
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public Optional<InterprocCallPlan> plan(RawTree rawTree,
                                            AnalysisStep step,
                                            MethodCallExpr methodCallExpr,
                                            EngineContext context) {
        if (!(step.getPayload() instanceof StatementsPayload currentPayload)) {
            return Optional.empty();
        }

        analysisTrace.log("INTERPROC: try top-level call " + methodCallExpr);

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
                step.copyBindings()
        );

        StatementsPayload targetPayload = StatementsPayload.from(targetInvocation.targetMethod)
                .withContinuationOnFinish(returnToCaller);

        Continuation targetMethodContinuation = new Continuation(
                targetInvocation.targetMethod,
                targetPayload,
                targetInvocation.entryAnchor,
                targetInvocation.targetBindings
        );

        analysisTrace.log("INTERPROC: planned top-level call into "
                + targetInvocation.targetMethod.getNameAsString()
                + ", entry rawNode#" + targetInvocation.entryAnchor.getId());

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

        analysisTrace.log("INTERPROC: try value-call " + methodCallExpr
                + " -> bind to variable " + targetVariableName);

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

        ExpressionResolutionResult returnResult = interprocReturnResolver.resolveReturnValue(
                rawTree,
                targetInvocation.targetMethod,
                targetInvocation.targetBindings,
                targetInvocation.entryAnchor,
                context
        );
        Map<String, ValueBinding> callerBindings = new HashMap<>(step.getBindings());
        ValueBinding boundReturnBinding = null;
        if (returnResult.isEmpty()) {
            analysisTrace.log("INTERPROC: value-call has no rebindable return, preserving callee body reads for "
                    + methodCallExpr);
        } else {
            boundReturnBinding = bindReturnValue(rawTree, targetVariableName, returnResult);
            callerBindings.put(targetVariableName, boundReturnBinding);
        }

        StatementsPayload afterCallPayload = currentPayload.next();
        Continuation returnToCaller = new Continuation(
                step.getMethod(),
                afterCallPayload,
                step.getCurrentRawNode(),
                callerBindings
        );

        StatementsPayload targetPayload = StatementsPayload.from(targetInvocation.targetMethod)
                .withContinuationOnFinish(returnToCaller);

        // Contract for value-call planning:
        // 1) eagerly resolve the callee return value so the caller variable can be rebound before
        //    caller-side continuation (e.g. `Address address = helper(document); address.getCity();`);
        // 2) still traverse the callee body because helper-side expression statements may carry
        //    meaningful usage paths (side effects / helper-body reads) that must remain observable;
        // 3) callee traversal must not reclassify the eagerly rebound return anchor as a standalone
        //    terminal path when the caller continues from that returned entity.
        Continuation targetMethodContinuation = new Continuation(
                targetInvocation.targetMethod,
                targetPayload,
                targetInvocation.entryAnchor,
                targetInvocation.targetBindings
        );

        analysisTrace.log("INTERPROC: planned value-call into "
                + targetInvocation.targetMethod.getNameAsString()
                + (boundReturnBinding == null
                ? ", without caller rebinding for variable " + targetVariableName
                : ", return bound to variable " + targetVariableName
                + " via nodes="
                + boundReturnBinding.getNodes().stream().map(node -> String.valueOf(node.getId())).toList()));

        return Optional.of(new InterprocCallPlan(targetMethodContinuation));
    }

    private Optional<TargetInvocation> resolveTargetInvocation(RawTree rawTree,
                                                               AnalysisStep step,
                                                               MethodCallExpr methodCallExpr,
                                                               EngineContext context) {
        Optional<List<TargetInvocation>> fanOutInvocations = resolveFanOutTargetInvocations(rawTree, step, methodCallExpr, context);
        if (fanOutInvocations.isPresent()) {
            List<TargetInvocation> invocations = fanOutInvocations.get();
            if (invocations.size() == 1) {
                return Optional.of(invocations.get(0));
            }
            return Optional.empty();
        }

        Optional<MethodDeclaration> targetMethodOpt = interprocMethodResolver.resolve(methodCallExpr, step);
        if (targetMethodOpt.isEmpty()) {
            return Optional.empty();
        }

        MethodDeclaration targetMethod = targetMethodOpt.get();
        if (targetMethod.getBody().isEmpty()) {
            return Optional.empty();
        }

        Optional<Map<String, ValueBinding>> targetBindingsOpt = interprocArgumentBinder.bindArguments(
                rawTree,
                step,
                methodCallExpr,
                targetMethod,
                context
        );
        if (targetBindingsOpt.isEmpty()) {
            return Optional.empty();
        }

        Map<String, ValueBinding> targetBindings = targetBindingsOpt.get();
        RawNode entryAnchor = resolveEntryAnchor(targetBindings, step.getCurrentRawNode());

        return Optional.of(new TargetInvocation(targetMethod, targetBindings, entryAnchor));
    }

    public Optional<InterprocCallPlan> planFanOut(RawTree rawTree,
                                                  AnalysisStep step,
                                                  MethodCallExpr methodCallExpr,
                                                  EngineContext context) {
        if (!(step.getPayload() instanceof StatementsPayload currentPayload)) {
            return Optional.empty();
        }

        Optional<List<TargetInvocation>> targetInvocationsOpt = resolveFanOutTargetInvocations(rawTree, step, methodCallExpr, context);
        if (targetInvocationsOpt.isEmpty()) {
            return Optional.empty();
        }

        List<TargetInvocation> targetInvocations = targetInvocationsOpt.get();
        if (targetInvocations.isEmpty()) {
            return Optional.empty();
        }

        StatementsPayload afterCallPayload = currentPayload.next();
        List<Continuation> continuations = new ArrayList<>();

        for (TargetInvocation targetInvocation : targetInvocations) {
            Continuation returnToCaller = new Continuation(
                    step.getMethod(),
                    afterCallPayload,
                    step.getCurrentRawNode(),
                    step.copyBindings()
            );

            StatementsPayload targetPayload = StatementsPayload.from(targetInvocation.targetMethod)
                    .withContinuationOnFinish(returnToCaller);

            continuations.add(new Continuation(
                    targetInvocation.targetMethod,
                    targetPayload,
                    targetInvocation.entryAnchor,
                    targetInvocation.targetBindings
            ));
        }

        return Optional.of(new InterprocCallPlan(continuations));
    }

    private Optional<List<TargetInvocation>> resolveFanOutTargetInvocations(RawTree rawTree,
                                                                            AnalysisStep step,
                                                                            MethodCallExpr methodCallExpr,
                                                                            EngineContext context) {
        if (methodCallExpr.getScope().isEmpty() || !methodCallExpr.getScope().get().isNameExpr()) {
            return Optional.empty();
        }

        String scopeName = methodCallExpr.getScope().get().asNameExpr().getNameAsString();
        ValueBinding binding = step.getBinding(scopeName);
        if (binding == null || !binding.hasDispatchTargets()) {
            return Optional.empty();
        }

        List<MethodDeclaration> targetMethods = interprocMethodResolver.resolveAllOnConcreteClasses(
                methodCallExpr,
                binding.getDispatchTargetClassNames()
        );
        if (targetMethods.isEmpty()) {
            return Optional.of(List.of());
        }

        List<TargetInvocation> invocations = new ArrayList<>();
        for (MethodDeclaration targetMethod : targetMethods) {
            if (targetMethod.getBody().isEmpty()) {
                continue;
            }

            Optional<Map<String, ValueBinding>> targetBindingsOpt = interprocArgumentBinder.bindArguments(
                    rawTree,
                    step,
                    methodCallExpr,
                    targetMethod,
                    context
            );
            if (targetBindingsOpt.isEmpty()) {
                continue;
            }

            Map<String, ValueBinding> targetBindings = targetBindingsOpt.get();
            RawNode entryAnchor = resolveEntryAnchor(targetBindings, step.getCurrentRawNode());
            invocations.add(new TargetInvocation(targetMethod, targetBindings, entryAnchor));
        }

        return Optional.of(invocations);
    }

    private ValueBinding bindReturnValue(RawTree rawTree,
                                         String variableName,
                                         ExpressionResolutionResult returnResult) {
        Set<RawNode> nodes = new LinkedHashSet<>();

        for (RawNode returnNode : returnResult.getNodes()) {
            RawNode boundNode = shouldCreateAlias(variableName, returnNode)
                    ? rawTree.addAlias(returnNode, variableName)
                    : returnNode;

            boundNode.setUsageKind(UsageKind.INTERMEDIATE);
            nodes.add(boundNode);
        }

        return new ValueBinding(nodes, returnResult.isUncertain());
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

    private RawNode resolveEntryAnchor(Map<String, ValueBinding> targetBindings, RawNode fallback) {
        long nonEmptyBindings = targetBindings.values().stream()
                .filter(binding -> binding != null && !binding.isEmpty())
                .count();

        if (nonEmptyBindings > 1) {
            return fallback;
        }

        for (ValueBinding binding : targetBindings.values()) {
            if (!binding.isEmpty()) {
                return binding.getNodes().iterator().next();
            }
        }
        return fallback;
    }

    private static class TargetInvocation {
        private final MethodDeclaration targetMethod;
        private final Map<String, ValueBinding> targetBindings;
        private final RawNode entryAnchor;

        private TargetInvocation(MethodDeclaration targetMethod,
                                 Map<String, ValueBinding> targetBindings,
                                 RawNode entryAnchor) {
            this.targetMethod = targetMethod;
            this.targetBindings = targetBindings;
            this.entryAnchor = entryAnchor;
        }
    }
}
