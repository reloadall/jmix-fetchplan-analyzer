package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

public final class LoadPlanLike {

    private LoadPlanLike() {
    }

    public static LoadPlanLike of(String name) {
        return new LoadPlanLike();
    }
}