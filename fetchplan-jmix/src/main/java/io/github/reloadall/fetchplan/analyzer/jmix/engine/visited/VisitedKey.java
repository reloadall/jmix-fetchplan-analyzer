package io.github.reloadall.fetchplan.analyzer.jmix.engine.visited;

import java.util.Objects;

public class VisitedKey {

    private final String methodKey;
    private final String payloadKey;
    private final Long currentRawNodeId;
    private final String bindingsKey;

    public VisitedKey(String methodKey,
                      String payloadKey,
                      Long currentRawNodeId,
                      String bindingsKey) {
        this.methodKey = Objects.requireNonNull(methodKey, "methodKey is null");
        this.payloadKey = Objects.requireNonNull(payloadKey, "payloadKey is null");
        this.currentRawNodeId = Objects.requireNonNull(currentRawNodeId, "currentRawNodeId is null");
        this.bindingsKey = Objects.requireNonNull(bindingsKey, "bindingsKey is null");
    }

    public String getMethodKey() {
        return methodKey;
    }

    public String getPayloadKey() {
        return payloadKey;
    }

    public Long getCurrentRawNodeId() {
        return currentRawNodeId;
    }

    public String getBindingsKey() {
        return bindingsKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisitedKey that)) return false;
        return methodKey.equals(that.methodKey)
                && payloadKey.equals(that.payloadKey)
                && currentRawNodeId.equals(that.currentRawNodeId)
                && bindingsKey.equals(that.bindingsKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodKey, payloadKey, currentRawNodeId, bindingsKey);
    }

    @Override
    public String toString() {
        return "VisitedKey{" +
                "methodKey='" + methodKey + '\'' +
                ", payloadKey='" + payloadKey + '\'' +
                ", currentRawNodeId=" + currentRawNodeId +
                ", bindingsKey='" + bindingsKey + '\'' +
                '}';
    }
}
