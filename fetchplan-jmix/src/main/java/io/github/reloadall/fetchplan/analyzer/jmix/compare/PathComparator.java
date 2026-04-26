package io.github.reloadall.fetchplan.analyzer.jmix.compare;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanPathSet;
import org.springframework.stereotype.Component;

@Component("fpa_PathComparator")
public class PathComparator {

    private static final Set<String> IGNORED_SYSTEM_FIELD_NAMES = Set.of(
            "id",
            "version",
            "createTs",
            "createdBy",
            "updateTs",
            "updatedBy",
            "deleteTs",
            "deletedBy"
    );

    public PathComparisonResult compare(Set<String> analyzedPaths,
                                        FetchPlanPathSet fetchPlanPathSet,
                                        Set<String> uncertainPaths) {
        Objects.requireNonNull(analyzedPaths, "analyzedPaths is null");
        Objects.requireNonNull(fetchPlanPathSet, "fetchPlanPathSet is null");
        Objects.requireNonNull(uncertainPaths, "uncertainPaths is null");

        Set<String> allDeclaredPaths = filterIgnoredSystemLeafPaths(fetchPlanPathSet.getAllPaths());
        Set<String> declaredLeafPaths = filterIgnoredSystemLeafPaths(fetchPlanPathSet.getLeafPaths());
        Set<String> effectiveAnalyzedPaths = filterIgnoredSystemLeafPaths(analyzedPaths);
        Set<String> covered = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();

        for (String analyzedPath : effectiveAnalyzedPaths) {
            if (isCoveredByFetchPlan(analyzedPath, allDeclaredPaths, declaredLeafPaths)) {
                covered.add(analyzedPath);
            } else {
                missing.add(analyzedPath);
            }
        }

        Set<String> extra = new LinkedHashSet<>();
        Set<String> finalUncertain = new LinkedHashSet<>(uncertainPaths);
        Set<String> declaredUnderUncertainty = new LinkedHashSet<>();
        Set<String> possibleAnalyzerGap = new LinkedHashSet<>();
        Set<String> probableOverfetch = new LinkedHashSet<>();

        for (String declaredLeafPath : declaredLeafPaths) {
            if (effectiveAnalyzedPaths.contains(declaredLeafPath)) {
                continue;
            }
            if (isUnderUncertainPrefix(declaredLeafPath, uncertainPaths)) {
                finalUncertain.add(declaredLeafPath);
                declaredUnderUncertainty.add(declaredLeafPath);
                continue;
            }
            extra.add(declaredLeafPath);
            if (isPossibleAnalyzerGap(declaredLeafPath, effectiveAnalyzedPaths)) {
                possibleAnalyzerGap.add(declaredLeafPath);
            } else {
                probableOverfetch.add(declaredLeafPath);
            }
        }

        Set<String> normalizedMissing = removeStructuralParents(missing);
        Set<String> structuralContainerPaths = new LinkedHashSet<>(allDeclaredPaths);
        structuralContainerPaths.removeAll(declaredLeafPaths);

        return new PathComparisonResult(
                covered,
                normalizedMissing,
                extra,
                finalUncertain,
                new DeclaredPathBreakdown(
                        structuralContainerPaths,
                        declaredUnderUncertainty,
                        possibleAnalyzerGap,
                        probableOverfetch
                )
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
            if (path.equals(uncertainPath)
                    || path.startsWith(uncertainPath + ".")
                    || uncertainPath.startsWith(path + ".")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> filterIgnoredSystemLeafPaths(Set<String> paths) {
        Set<String> result = new LinkedHashSet<>();
        for (String path : paths) {
            if (!isIgnoredSystemLeafPath(path)) {
                result.add(path);
            }
        }
        return result;
    }

    private boolean isIgnoredSystemLeafPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        List<String> segments = List.of(path.split("\\."));
        if (segments.isEmpty()) {
            return false;
        }
        String leaf = segments.get(segments.size() - 1);
        return IGNORED_SYSTEM_FIELD_NAMES.contains(leaf);
    }

    private boolean isPossibleAnalyzerGap(String declaredLeafPath, Set<String> analyzedPaths) {
        int lastDot = declaredLeafPath.lastIndexOf('.');
        if (lastDot <= 0) {
            return false;
        }

        String parentPrefix = declaredLeafPath.substring(0, lastDot);
        String siblingPrefix = parentPrefix + ".";
        for (String analyzedPath : analyzedPaths) {
            if (!analyzedPath.equals(declaredLeafPath) && analyzedPath.startsWith(siblingPrefix)) {
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
