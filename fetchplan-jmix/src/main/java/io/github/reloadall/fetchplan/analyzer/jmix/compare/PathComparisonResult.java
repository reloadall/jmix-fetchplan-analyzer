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

    public PathComparisonResult(Set<String> matchedPaths,
                                Set<String> missingPaths,
                                Set<String> extraPaths,
                                Set<String> uncertainPaths) {
        this.matchedPaths = unmodifiableCopy(matchedPaths);
        this.missingPaths = unmodifiableCopy(missingPaths);
        this.extraPaths = unmodifiableCopy(extraPaths);
        this.uncertainPaths = unmodifiableCopy(uncertainPaths);
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

    private Set<String> unmodifiableCopy(Set<String> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
