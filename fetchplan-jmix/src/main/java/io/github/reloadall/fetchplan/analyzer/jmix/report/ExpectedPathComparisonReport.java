package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExpectedPathComparisonReport {

    private final AnalysisTarget target;
    private final Set<String> addonCanonicalPaths;
    private final Set<String> expectedPaths;
    private final Set<String> matchedPaths;
    private final Set<String> missingPaths;
    private final Set<String> extraPaths;
    private final List<ReportUnsupported> unsupportedConstructs;
    private final List<ReportWarning> warnings;
    private final Set<String> analysisLimits;

    public ExpectedPathComparisonReport(AnalysisTarget target,
                                        Set<String> addonCanonicalPaths,
                                        Set<String> expectedPaths,
                                        Set<String> matchedPaths,
                                        Set<String> missingPaths,
                                        Set<String> extraPaths,
                                        List<ReportUnsupported> unsupportedConstructs,
                                        List<ReportWarning> warnings,
                                        Set<String> analysisLimits) {
        this.target = Objects.requireNonNull(target, "target is null");
        this.addonCanonicalPaths = copy(addonCanonicalPaths);
        this.expectedPaths = copy(expectedPaths);
        this.matchedPaths = copy(matchedPaths);
        this.missingPaths = copy(missingPaths);
        this.extraPaths = copy(extraPaths);
        this.unsupportedConstructs = List.copyOf(Objects.requireNonNull(unsupportedConstructs, "unsupportedConstructs is null"));
        this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings is null"));
        this.analysisLimits = copy(analysisLimits);
    }

    public AnalysisTarget getTarget() {
        return target;
    }

    public Set<String> getAddonCanonicalPaths() {
        return addonCanonicalPaths;
    }

    public Set<String> getExpectedPaths() {
        return expectedPaths;
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

    public List<ReportUnsupported> getUnsupportedConstructs() {
        return unsupportedConstructs;
    }

    public List<ReportWarning> getWarnings() {
        return warnings;
    }

    public Set<String> getAnalysisLimits() {
        return analysisLimits;
    }

    private Set<String> copy(Set<String> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}