package io.github.reloadall.fetchplan.analyzer.jmix.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public class RawTree {

    private RawNode root;
    private final Map<Long, RawNode> nodes;
    private long sequence;

    public RawTree() {
        this.nodes = new LinkedHashMap<>();
    }

    public RawNode getRoot() {
        return root;
    }

    public Map<Long, RawNode> getNodes() {
        return nodes;
    }

    public long getSequence() {
        return sequence;
    }

    public RawNode createRoot(String variableName) {
        if (root != null) {
            throw new IllegalStateException("Root already exists");
        }

        RawNode rootNode = new RawNode(
                nextId(),
                null,
                FlowKind.ROOT,
                null,
                variableName,
                null,
                UsageKind.NONE
        );

        this.root = rootNode;
        this.nodes.put(rootNode.getId(), rootNode);
        return rootNode;
    }

    public RawNode addChild(RawNode parent,
                            String entityField,
                            FlowKind flowKind,
                            String variableName,
                            UsageKind usageKind) {
        if (parent == null) {
            throw new IllegalArgumentException("parent is null");
        }
        if (flowKind == null) {
            throw new IllegalArgumentException("flowKind is null");
        }
        if (usageKind == null) {
            throw new IllegalArgumentException("usageKind is null");
        }

        String childKey = buildChildKey(entityField, flowKind, variableName, parent.getId());
        RawNode existing = parent.getChild(childKey);
        if (existing != null) {
            existing.setUsageKind(usageKind);
            return existing;
        }

        RawNode child = new RawNode(
                nextId(),
                parent,
                flowKind,
                entityField,
                variableName,
                parent.getId(),
                usageKind
        );

        parent.putChild(childKey, child);
        nodes.put(child.getId(), child);
        return child;
    }

    public RawNode addAlias(RawNode sourceNode, String variableName) {
        if (sourceNode == null) {
            throw new IllegalArgumentException("sourceNode is null");
        }
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("variableName is blank");
        }

        return addChild(
                sourceNode,
                null,
                FlowKind.ALIAS,
                variableName,
                UsageKind.INTERMEDIATE
        );
    }

    public RawNode addUnknownBreak(RawNode parent, String variableName, UsageKind usageKind) {
        if (parent == null) {
            throw new IllegalArgumentException("parent is null");
        }
        if (usageKind == null) {
            throw new IllegalArgumentException("usageKind is null");
        }

        return addChild(
                parent,
                null,
                FlowKind.UNKNOWN_BREAK,
                variableName,
                usageKind
        );
    }

    public RawNode getNode(Long id) {
        return nodes.get(id);
    }

    private long nextId() {
        sequence++;
        return sequence;
    }

    private String buildChildKey(String entityField,
                                 FlowKind flowKind,
                                 String variableName,
                                 Long sourceNodeId) {
        if (entityField != null) {
            return "FIELD:" + entityField;
        }

        return "TECH:" +
                safe(flowKind != null ? flowKind.name() : null) + ":" +
                safe(variableName) + ":" +
                safe(sourceNodeId);
    }

    private String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
