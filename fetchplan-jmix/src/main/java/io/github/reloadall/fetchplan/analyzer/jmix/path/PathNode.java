package io.github.reloadall.fetchplan.analyzer.jmix.path;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PathNode {

    private final String segment;
    private final Map<String, PathNode> children;
    private boolean explicitTerminal;
    private boolean leafCandidate;
    private boolean blockedByUncertainty;

    public PathNode(String segment) {
        this.segment = segment;
        this.children = new LinkedHashMap<>();
    }

    public String getSegment() {
        return segment;
    }

    public Map<String, PathNode> getChildren() {
        return children;
    }

    public boolean isTerminal() {
        return explicitTerminal;
    }

    public void setTerminal(boolean terminal) {
        this.explicitTerminal = terminal;
    }

    public boolean isExplicitTerminal() {
        return explicitTerminal;
    }

    public boolean isLeafCandidate() {
        return leafCandidate;
    }

    public void setLeafCandidate(boolean leafCandidate) {
        this.leafCandidate = leafCandidate;
    }

    public boolean isBlockedByUncertainty() {
        return blockedByUncertainty;
    }

    public void setBlockedByUncertainty(boolean blockedByUncertainty) {
        this.blockedByUncertainty = blockedByUncertainty;
    }

    public PathNode getOrCreateChild(String segment) {
        Objects.requireNonNull(segment, "segment is null");
        return children.computeIfAbsent(segment, PathNode::new);
    }

    @Override
    public String toString() {
        return "PathNode{" +
                "segment='" + segment + '\'' +
                ", explicitTerminal=" + explicitTerminal +
                ", leafCandidate=" + leafCandidate +
                ", blockedByUncertainty=" + blockedByUncertainty +
                ", children=" + children.keySet() +
                '}';
    }
}
