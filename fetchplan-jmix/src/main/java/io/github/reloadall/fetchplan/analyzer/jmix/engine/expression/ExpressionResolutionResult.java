package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;

public class ExpressionResolutionResult {
    private final Set<RawNode> nodes;
    private final boolean uncertain;

    public ExpressionResolutionResult(Set<RawNode> nodes, boolean uncertain) {
        Objects.requireNonNull(nodes, "nodes is null");
        this.nodes = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
        this.uncertain = uncertain;
    }

    public static ExpressionResolutionResult empty() {
        return new ExpressionResolutionResult(Set.of(), false);
    }

    public static ExpressionResolutionResult uncertainEmpty() {
        return new ExpressionResolutionResult(Set.of(), true);
    }

    public static ExpressionResolutionResult of(RawNode node) {
        Objects.requireNonNull(node, "node is null");
        return new ExpressionResolutionResult(Set.of(node), false);
    }

    public static ExpressionResolutionResult of(Set<RawNode> nodes) {
        return new ExpressionResolutionResult(nodes, false);
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

    public ExpressionResolutionResult merge(ExpressionResolutionResult other) {
        Objects.requireNonNull(other, "other is null");

        Set<RawNode> merged = new LinkedHashSet<>(this.nodes);
        merged.addAll(other.nodes);

        return new ExpressionResolutionResult(
                merged,
                this.uncertain || other.uncertain
        );
    }
}
