package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;

public class AnalysisStep {

    private final MethodDeclaration method;
    private final StepPayload payload;
    private final RawNode currentRawNode;
    private final Map<String, ValueBinding> bindings;

    public AnalysisStep(MethodDeclaration method,
                        StepPayload payload,
                        RawNode currentRawNode,
                        Map<String, ValueBinding> bindings) {
        this.method = Objects.requireNonNull(method, "method is null");
        this.payload = Objects.requireNonNull(payload, "payload is null");
        this.currentRawNode = Objects.requireNonNull(currentRawNode, "currentRawNode is null");
        this.bindings = new LinkedHashMap<>(Objects.requireNonNull(bindings, "bindings is null"));
    }

    public static AnalysisStep start(MethodDeclaration method,
                                     String rootParamName,
                                     RawNode rootNode) {
        Map<String, ValueBinding> bindings = new LinkedHashMap<>();
        bindings.put(rootParamName, ValueBinding.of(rootNode));

        return new AnalysisStep(
                method,
                StatementsPayload.from(method),
                rootNode,
                bindings
        );
    }

    public MethodDeclaration getMethod() {
        return method;
    }

    public StepPayload getPayload() {
        return payload;
    }

    public RawNode getCurrentRawNode() {
        return currentRawNode;
    }

    public Map<String, ValueBinding> getBindings() {
        return bindings;
    }

    public ValueBinding getBinding(String name) {
        return bindings.get(name);
    }

    public void bind(String name, ValueBinding binding) {
        bindings.put(name, binding);
    }

    public void bindSingle(String name, RawNode node) {
        bindings.put(name, ValueBinding.of(node));
    }

    public void bindAll(String name, ExpressionResolutionResult result) {
        bindings.put(name, ValueBinding.from(result));
    }

    public Continuation continueWith(StepPayload nextPayload) {
        return new Continuation(
                method,
                nextPayload,
                currentRawNode,
                copyBindings()
        );
    }

    public Continuation continueWith(MethodDeclaration method,
                                     StepPayload payload,
                                     RawNode currentRawNode,
                                     Map<String, ValueBinding> bindings) {
        return new Continuation(
                method,
                payload,
                currentRawNode,
                bindings
        );
    }

    public Map<String, ValueBinding> copyBindings() {
        return new LinkedHashMap<>(bindings);
    }
}
