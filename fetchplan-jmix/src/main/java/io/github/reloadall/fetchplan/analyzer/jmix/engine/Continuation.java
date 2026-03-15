package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;

public class Continuation {

    private final MethodDeclaration method;
    private final StepPayload payload;
    private final RawNode currentRawNode;
    private final Map<String, RawNode> bindings;

    public Continuation(MethodDeclaration method,
                        StepPayload payload,
                        RawNode currentRawNode,
                        Map<String, RawNode> bindings) {
        this.method = Objects.requireNonNull(method, "method is null");
        this.payload = Objects.requireNonNull(payload, "payload is null");
        this.currentRawNode = Objects.requireNonNull(currentRawNode, "currentRawNode is null");
        this.bindings = new HashMap<>(Objects.requireNonNull(bindings, "bindings is null"));
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

    public Map<String, RawNode> getBindings() {
        return bindings;
    }

    public AnalysisStep toStep() {
        return new AnalysisStep(
                method,
                payload,
                currentRawNode,
                new HashMap<>(bindings)
        );
    }
}
