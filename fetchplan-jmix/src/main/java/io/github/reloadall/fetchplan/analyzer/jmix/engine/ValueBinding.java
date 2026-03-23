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

    public ValueBinding(Set<RawNode> nodes, boolean uncertain) {
        Objects.requireNonNull(nodes, "nodes is null");
        this.nodes = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
        this.uncertain = uncertain;
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

    public Set<RawNode> getNodes() {
        return nodes;
    }

    public boolean isUncertain() {
        return uncertain;
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

        return new ValueBinding(merged, this.uncertain || other.uncertain);
    }
}
