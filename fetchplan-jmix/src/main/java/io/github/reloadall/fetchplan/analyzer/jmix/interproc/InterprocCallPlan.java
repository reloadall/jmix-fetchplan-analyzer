package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;

public class InterprocCallPlan {

    private final List<Continuation> targetMethodContinuations;

    public InterprocCallPlan(Continuation targetMethodContinuation) {
        this(List.of(targetMethodContinuation));
    }

    public InterprocCallPlan(List<Continuation> targetMethodContinuations) {
        Objects.requireNonNull(targetMethodContinuations, "targetMethodContinuations is null");
        this.targetMethodContinuations = Collections.unmodifiableList(new ArrayList<>(targetMethodContinuations));
    }

    public Continuation getTargetMethodContinuation() {
        if (targetMethodContinuations.size() != 1) {
            throw new IllegalStateException("Expected exactly one target continuation, actual = " + targetMethodContinuations.size());
        }
        return targetMethodContinuations.get(0);
    }

    public List<Continuation> getTargetMethodContinuations() {
        return targetMethodContinuations;
    }
}
