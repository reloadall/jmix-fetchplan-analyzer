package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component("fpa_ExpectedPathComparisonService")
public class ExpectedPathComparisonService {

    public ExpectedPathComparisonReport compare(AnalysisReport report, Set<String> expectedPaths) {
        Objects.requireNonNull(report, "report is null");

        Set<String> addonCanonicalPaths = normalize(report.getAnalyzedPaths());
        Set<String> normalizedExpectedPaths = normalize(expectedPaths == null ? Set.of() : expectedPaths);

        Set<String> matchedPaths = new LinkedHashSet<>(addonCanonicalPaths);
        matchedPaths.retainAll(normalizedExpectedPaths);

        Set<String> missingPaths = new LinkedHashSet<>(normalizedExpectedPaths);
        missingPaths.removeAll(addonCanonicalPaths);

        Set<String> extraPaths = new LinkedHashSet<>(addonCanonicalPaths);
        extraPaths.removeAll(normalizedExpectedPaths);

        return new ExpectedPathComparisonReport(
                report.getTarget(),
                addonCanonicalPaths,
                normalizedExpectedPaths,
                matchedPaths,
                missingPaths,
                extraPaths,
                report.getUnsupportedConstructs(),
                report.getWarnings(),
                report.getAnalysisLimits()
        );
    }

    private Set<String> normalize(Set<String> paths) {
        Set<String> trimmed = new TreeSet<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            trimmed.add(path.trim());
        }
        Set<String> result = new LinkedHashSet<>();
        result.addAll(trimmed);
        return result;
    }
}