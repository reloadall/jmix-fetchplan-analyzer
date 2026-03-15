package io.github.reloadall.fetchplan.analyzer.jmix.tree;

import java.util.LinkedHashMap;
import java.util.Map;

public class RawNode {

    private final Long id;
    private final RawNode parent;
    private final Map<String, RawNode> children;
    private final FlowKind flowKind;
    private final String entityField;
    private final String variableName;
    private final Long sourceNodeId;
    private UsageKind usageKind;

    public RawNode(Long id,
                   RawNode parent,
                   FlowKind flowKind,
                   String entityField,
                   String variableName,
                   Long sourceNodeId,
                   UsageKind usageKind) {
        this.id = id;
        this.parent = parent;
        this.children = new LinkedHashMap<>();
        this.flowKind = flowKind;
        this.entityField = entityField;
        this.variableName = variableName;
        this.sourceNodeId = sourceNodeId;
        this.usageKind = usageKind;
    }

    public Long getId() {
        return id;
    }

    public RawNode getParent() {
        return parent;
    }

    public Map<String, RawNode> getChildren() {
        return children;
    }

    public FlowKind getFlowKind() {
        return flowKind;
    }

    public String getEntityField() {
        return entityField;
    }

    public String getVariableName() {
        return variableName;
    }

    public Long getSourceNodeId() {
        return sourceNodeId;
    }

    public UsageKind getUsageKind() {
        return usageKind;
    }

    public void setUsageKind(UsageKind usageKind) {
        if (usageKind == null) {
            return;
        }

        if (this.usageKind == null || rank(usageKind) > rank(this.usageKind)) {
            this.usageKind = usageKind;
        }
    }

    public boolean isTechnical() {
        return entityField == null;
    }

    public RawNode getChild(String key) {
        return children.get(key);
    }

    public void putChild(String key, RawNode child) {
        children.put(key, child);
    }

    private int rank(UsageKind usageKind) {
        if (usageKind == UsageKind.NONE) {
            return 0;
        }
        if (usageKind == UsageKind.INTERMEDIATE) {
            return 1;
        }
        if (usageKind == UsageKind.TERMINAL) {
            return 2;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "RawNode{" +
                "id=" + id +
                ", parent=" + (parent != null ? parent.id : null) +
                ", flowKind=" + flowKind +
                ", entityField='" + entityField + '\'' +
                ", variableName='" + variableName + '\'' +
                ", sourceNodeId=" + sourceNodeId +
                ", usageKind=" + usageKind +
                '}';
    }

}
