package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.Objects;

public class ResolvedTargetType {

    private final String declaredTypeName;
    private final String injectionPointName;
    private final boolean springBeanCandidate;

    public ResolvedTargetType(String declaredTypeName,
                              String injectionPointName,
                              boolean springBeanCandidate) {
        this.declaredTypeName = Objects.requireNonNull(declaredTypeName, "declaredTypeName is null");
        this.injectionPointName = injectionPointName;
        this.springBeanCandidate = springBeanCandidate;
    }

    public String getDeclaredTypeName() {
        return declaredTypeName;
    }

    public String getInjectionPointName() {
        return injectionPointName;
    }

    public boolean isSpringBeanCandidate() {
        return springBeanCandidate;
    }
}
