package io.github.reloadall.fetchplan.analyzer.jmix.compare;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class PathComparisonResult {

    private final Set<String> matchedPaths;
    private final Set<String> missingPaths;
    private final Set<String> extraPaths;
    private final Set<String> uncertainPaths;
    private final DeclaredPathBreakdown declaredPathBreakdown;

    public PathComparisonResult(Set<String> matchedPaths,
                                Set<String> missingPaths,
                                Set<String> extraPaths,
                                Set<String> uncertainPaths) {
        this(matchedPaths, missingPaths, extraPaths, uncertainPaths,
                new DeclaredPathBreakdown(Set.of(), Set.of(), Set.of(), Set.of()));
    }

    public PathComparisonResult(Set<String> matchedPaths,
                                Set<String> missingPaths,
                                Set<String> extraPaths,
                                Set<String> uncertainPaths,
                                DeclaredPathBreakdown declaredPathBreakdown) {
        this.matchedPaths = unmodifiableCopy(matchedPaths);
        this.missingPaths = unmodifiableCopy(missingPaths);
        this.extraPaths = unmodifiableCopy(extraPaths);
        this.uncertainPaths = unmodifiableCopy(uncertainPaths);
        this.declaredPathBreakdown = Objects.requireNonNull(declaredPathBreakdown, "declaredPathBreakdown is null");
    }

    public Set<String> getMatchedPaths() {
        return matchedPaths;
    }

    public Set<String> getMissingPaths() {
        return missingPaths;
    }

    public Set<String> getExtraPaths() {
        return extraPaths;
    }

    public Set<String> getUncertainPaths() {
        return uncertainPaths;
    }

    public DeclaredPathBreakdown getDeclaredPathBreakdown() {
        return declaredPathBreakdown;
    }

    private Set<String> unmodifiableCopy(Set<String> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
