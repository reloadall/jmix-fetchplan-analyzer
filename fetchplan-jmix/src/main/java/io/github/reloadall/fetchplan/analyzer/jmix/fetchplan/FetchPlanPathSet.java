package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class FetchPlanPathSet {

    private final Set<String> allPaths;
    private final Set<String> leafPaths;

    public FetchPlanPathSet(Set<String> allPaths, Set<String> leafPaths) {
        Objects.requireNonNull(allPaths, "allPaths is null");
        Objects.requireNonNull(leafPaths, "leafPaths is null");

        this.allPaths = Collections.unmodifiableSet(new LinkedHashSet<>(allPaths));
        this.leafPaths = Collections.unmodifiableSet(new LinkedHashSet<>(leafPaths));
    }

    public Set<String> getAllPaths() {
        return allPaths;
    }

    public Set<String> getLeafPaths() {
        return leafPaths;
    }
}
