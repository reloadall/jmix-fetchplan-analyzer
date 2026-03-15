package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class StatementHandleResult {

    private final boolean continueLinear;
    private final List<Continuation> continuations;

    private StatementHandleResult(boolean continueLinear, List<Continuation> continuations) {
        this.continueLinear = continueLinear;
        this.continuations = Collections.unmodifiableList(new ArrayList<>(continuations));
    }

    public static StatementHandleResult continueLinear() {
        return new StatementHandleResult(true, List.of());
    }

    public static StatementHandleResult stop() {
        return new StatementHandleResult(false, List.of());
    }

    public static StatementHandleResult customContinuations(List<Continuation> continuations) {
        Objects.requireNonNull(continuations, "continuations is null");
        return new StatementHandleResult(false, continuations);
    }

    public static StatementHandleResult continueLinearWith(List<Continuation> continuations) {
        Objects.requireNonNull(continuations, "continuations is null");
        return new StatementHandleResult(true, continuations);
    }

    public boolean isContinueLinear() {
        return continueLinear;
    }

    public List<Continuation> getContinuations() {
        return continuations;
    }
}
