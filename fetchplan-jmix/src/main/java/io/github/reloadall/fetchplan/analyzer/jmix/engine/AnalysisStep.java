package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;

public class AnalysisStep {

    private final MethodDeclaration method;
    private final StepPayload payload;
    private RawNode currentRawNode;
    private final Map<String, RawNode> bindings;

    public static AnalysisStep start(MethodDeclaration method, String rootParamName, RawNode rootNode) {
        Objects.requireNonNull(method, "method is null");
        Objects.requireNonNull(rootParamName, "rootParamName is null");
        Objects.requireNonNull(rootNode, "rootNode is null");

        Map<String, RawNode> bindings = new HashMap<>();
        bindings.put(rootParamName, rootNode);

        return new AnalysisStep(
                method,
                StatementsPayload.from(method),
                rootNode,
                bindings
        );
    }

    public AnalysisStep(MethodDeclaration method,
                        StepPayload payload,
                        RawNode currentRawNode,
                        Map<String, RawNode> bindings) {
        this.method = Objects.requireNonNull(method, "method is null");
        this.payload = Objects.requireNonNull(payload, "payload is null");
        this.currentRawNode = Objects.requireNonNull(currentRawNode, "currentRawNode is null");
        this.bindings = Objects.requireNonNull(bindings, "bindings is null");
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

    public void setCurrentRawNode(RawNode currentRawNode) {
        this.currentRawNode = Objects.requireNonNull(currentRawNode, "currentRawNode is null");
    }

    public Map<String, RawNode> getBindings() {
        return bindings;
    }

    public void bind(String variableName, RawNode node) {
        bindings.put(variableName, node);
    }

    public RawNode resolveBinding(String variableName) {
        return bindings.get(variableName);
    }

    public Continuation continueWith(StepPayload nextPayload) {
        return new Continuation(method, nextPayload, currentRawNode, bindings);
    }

    public Continuation continueWith(StepPayload nextPayload, RawNode nextCurrentRawNode) {
        return new Continuation(method, nextPayload, nextCurrentRawNode, bindings);
    }

    public Continuation continueWith(MethodDeclaration nextMethod,
                                     StepPayload nextPayload,
                                     RawNode nextCurrentRawNode,
                                     Map<String, RawNode> nextBindings) {
        return new Continuation(nextMethod, nextPayload, nextCurrentRawNode, nextBindings);
    }
}
