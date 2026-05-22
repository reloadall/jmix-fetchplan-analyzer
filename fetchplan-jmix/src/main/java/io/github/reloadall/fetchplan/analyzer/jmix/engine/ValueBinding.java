package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolutionResult;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;

public class ValueBinding {
    private final Set<RawNode> nodes;
    private final boolean uncertain;
    private final Set<String> dispatchTargetClassNames;
    private final boolean terminalOnly;

    public ValueBinding(Set<RawNode> nodes, boolean uncertain) {
        this(nodes, uncertain, Set.of(), false);
    }

    public ValueBinding(Set<RawNode> nodes,
                        boolean uncertain,
                        Set<String> dispatchTargetClassNames) {
        this(nodes, uncertain, dispatchTargetClassNames, false);
    }

    public ValueBinding(Set<RawNode> nodes,
                        boolean uncertain,
                        Set<String> dispatchTargetClassNames,
                        boolean terminalOnly) {
        Objects.requireNonNull(nodes, "nodes is null");
        Objects.requireNonNull(dispatchTargetClassNames, "dispatchTargetClassNames is null");
        this.nodes = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
        this.uncertain = uncertain;
        this.dispatchTargetClassNames = Collections.unmodifiableSet(new LinkedHashSet<>(dispatchTargetClassNames));
        this.terminalOnly = terminalOnly;
    }

    public static ValueBinding empty() {
        return new ValueBinding(Set.of(), false);
    }

    public static ValueBinding uncertainEmpty() {
        return new ValueBinding(Set.of(), true);
    }

    public static ValueBinding of(RawNode node) {
        Objects.requireNonNull(node, "node is null");
        return new ValueBinding(Set.of(node), false);
    }

    public static ValueBinding of(Set<RawNode> nodes) {
        return new ValueBinding(nodes, false);
    }

    public static ValueBinding from(ExpressionResolutionResult result) {
        Objects.requireNonNull(result, "result is null");
        return new ValueBinding(result.getNodes(), result.isUncertain());
    }

    public static ValueBinding terminalOnly(Set<RawNode> nodes, boolean uncertain) {
        return new ValueBinding(nodes, uncertain, Set.of(), true);
    }

    public static ValueBinding forDispatchTargets(Set<String> dispatchTargetClassNames, boolean uncertain) {
        return new ValueBinding(Set.of(), uncertain, dispatchTargetClassNames);
    }

    public Set<RawNode> getNodes() {
        return nodes;
    }

    public boolean isUncertain() {
        return uncertain;
    }

    public Set<String> getDispatchTargetClassNames() {
        return dispatchTargetClassNames;
    }

    public boolean hasDispatchTargets() {
        return !dispatchTargetClassNames.isEmpty();
    }

    public boolean isTerminalOnly() {
        return terminalOnly;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public boolean hasSingleNode() {
        return nodes.size() == 1;
    }

    public RawNode getSingleNodeOrNull() {
        return hasSingleNode() ? nodes.iterator().next() : null;
    }

    public ValueBinding merge(ValueBinding other) {
        Objects.requireNonNull(other, "other is null");

        Set<RawNode> merged = new LinkedHashSet<>(this.nodes);
        merged.addAll(other.nodes);

        Set<String> mergedDispatchTargets = new LinkedHashSet<>(this.dispatchTargetClassNames);
        mergedDispatchTargets.addAll(other.dispatchTargetClassNames);

        return new ValueBinding(
                merged,
                this.uncertain || other.uncertain,
                mergedDispatchTargets,
                this.terminalOnly || other.terminalOnly
        );
    }
}
