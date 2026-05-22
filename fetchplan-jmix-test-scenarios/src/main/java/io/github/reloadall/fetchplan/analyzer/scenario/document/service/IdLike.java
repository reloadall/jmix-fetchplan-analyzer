package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

public final class IdLike<T> {

    private IdLike() {
    }

    public static <T> IdLike<T> of(T value) {
        return new IdLike<>();
    }
}