package io.github.reloadall.fetchplan.analyzer.jmix.compare;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanPathSet;
import org.springframework.stereotype.Component;

@Component("fpa_PathComparator")
public class PathComparator {

    public PathComparisonResult compare(Set<String> analyzedPaths,
                                        FetchPlanPathSet fetchPlanPathSet,
                                        Set<String> uncertainPaths) {
        Objects.requireNonNull(analyzedPaths, "analyzedPaths is null");
        Objects.requireNonNull(fetchPlanPathSet, "fetchPlanPathSet is null");
        Objects.requireNonNull(uncertainPaths, "uncertainPaths is null");

        Set<String> allDeclaredPaths = fetchPlanPathSet.getAllPaths();
        Set<String> declaredLeafPaths = fetchPlanPathSet.getLeafPaths();
        Set<String> covered = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();

        for (String analyzedPath : analyzedPaths) {
            if (isCoveredByFetchPlan(analyzedPath, allDeclaredPaths, declaredLeafPaths)) {
                covered.add(analyzedPath);
            } else {
                missing.add(analyzedPath);
            }
        }

        Set<String> extra = new LinkedHashSet<>();
        Set<String> finalUncertain = new LinkedHashSet<>(uncertainPaths);

        for (String declaredLeafPath : declaredLeafPaths) {
            if (analyzedPaths.contains(declaredLeafPath)) {
                continue;
            }
            if (isUnderUncertainPrefix(declaredLeafPath, uncertainPaths)) {
                finalUncertain.add(declaredLeafPath);
                continue;
            }
            extra.add(declaredLeafPath);
        }

        Set<String> normalizedMissing = removeStructuralParents(missing);
        return new PathComparisonResult(
                covered,
                normalizedMissing,
                extra,
                finalUncertain
        );
    }

    private boolean isCoveredByFetchPlan(String analyzedPath,
                                         Set<String> allDeclaredPaths,
                                         Set<String> declaredLeafPaths) {
        if (allDeclaredPaths.contains(analyzedPath)) {
            return true;
        }
        String prefix = analyzedPath + ".";
        for (String declaredLeafPath : declaredLeafPaths) {
            if (declaredLeafPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnderUncertainPrefix(String path, Set<String> uncertainPaths) {
        for (String uncertainPath : uncertainPaths) {
            if ("<root>".equals(uncertainPath)) {
                return true;
            }
            if (path.equals(uncertainPath) || path.startsWith(uncertainPath + ".")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> removeStructuralParents(Set<String> paths) {
        Set<String> result = new LinkedHashSet<>();
        for (String candidate : paths) {
            if (!hasDeeperDescendant(candidate, paths)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean hasDeeperDescendant(String parentPath, Set<String> paths) {
        String prefix = parentPath + ".";
        for (String path : paths) {
            if (!path.equals(parentPath) && path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
