package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.Objects;

import io.github.reloadall.fetchplan.analyzer.jmix.engine.Continuation;

public class InterprocCallPlan {

    private final Continuation targetMethodContinuation;

    public InterprocCallPlan(Continuation targetMethodContinuation) {
        this.targetMethodContinuation = Objects.requireNonNull(
                targetMethodContinuation,
                "targetMethodContinuation is null"
        );
    }

    public Continuation getTargetMethodContinuation() {
        return targetMethodContinuation;
    }
}
