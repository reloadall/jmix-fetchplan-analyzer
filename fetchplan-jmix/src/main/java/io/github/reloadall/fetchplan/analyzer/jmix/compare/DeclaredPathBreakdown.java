package io.github.reloadall.fetchplan.analyzer.jmix.compare;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class DeclaredPathBreakdown {

    private final Set<String> structuralContainerPaths;
    private final Set<String> declaredUnderUncertainty;
    private final Set<String> possibleAnalyzerGap;
    private final Set<String> probableOverfetch;

    public DeclaredPathBreakdown(Set<String> structuralContainerPaths,
                                 Set<String> declaredUnderUncertainty,
                                 Set<String> possibleAnalyzerGap,
                                 Set<String> probableOverfetch) {
        this.structuralContainerPaths = copy(structuralContainerPaths);
        this.declaredUnderUncertainty = copy(declaredUnderUncertainty);
        this.possibleAnalyzerGap = copy(possibleAnalyzerGap);
        this.probableOverfetch = copy(probableOverfetch);
    }

    public Set<String> getStructuralContainerPaths() {
        return structuralContainerPaths;
    }

    public Set<String> getDeclaredUnderUncertainty() {
        return declaredUnderUncertainty;
    }

    public Set<String> getPossibleAnalyzerGap() {
        return possibleAnalyzerGap;
    }

    public Set<String> getProbableOverfetch() {
        return probableOverfetch;
    }

    private Set<String> copy(Set<String> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}