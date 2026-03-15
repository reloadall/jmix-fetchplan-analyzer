package io.github.reloadall.fetchplan.analyzer.jmix.path;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PathNode {

    private final String segment;
    private final Map<String, PathNode> children;
    private boolean terminal;

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
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public PathNode getOrCreateChild(String segment) {
        Objects.requireNonNull(segment, "segment is null");
        return children.computeIfAbsent(segment, PathNode::new);
    }

    @Override
    public String toString() {
        return "PathNode{" +
                "segment='" + segment + '\'' +
                ", terminal=" + terminal +
                ", children=" + children.keySet() +
                '}';
    }
}
