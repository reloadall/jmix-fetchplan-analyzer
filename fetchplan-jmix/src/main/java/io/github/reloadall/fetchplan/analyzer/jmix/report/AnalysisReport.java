package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparisonResult;

public class AnalysisReport {

    private final AnalysisTarget target;
    private final List<ReportPath> canonicalPaths;
    private final List<ReportUnsupported> unsupportedConstructs;
    private final List<ReportWarning> warnings;
    private final Set<String> analysisLimits;

    private final String targetClass;
    private final String methodName;
    private final String rootParamName;
    private final String rootType;
    private final String fetchPlanName;
    private final Set<String> analyzedPaths;
    private final Set<String> declaredFetchPlanPaths;
    private final PathComparisonResult comparisonResult;

    public AnalysisReport(AnalysisTarget target,
                          List<ReportPath> canonicalPaths,
                          List<ReportUnsupported> unsupportedConstructs,
                          List<ReportWarning> warnings,
                          Set<String> analysisLimits) {
        this.target = Objects.requireNonNull(target, "target is null");
        this.canonicalPaths = copyList(canonicalPaths);
        this.unsupportedConstructs = copyList(unsupportedConstructs);
        this.warnings = copyList(warnings);
        this.analysisLimits = copy(analysisLimits == null ? Set.of() : analysisLimits);

        this.targetClass = target.getClassName();
        this.methodName = target.getMethodName();
        this.rootParamName = target.getRootParameterName();
        this.rootType = target.getRootType();
        this.fetchPlanName = null;
        this.analyzedPaths = copy(pathsFrom(canonicalPaths));
        this.declaredFetchPlanPaths = Set.of();
        this.comparisonResult = null;
    }

    public AnalysisReport(String targetClass,
                          String methodName,
                          String rootParamName,
                          String fetchPlanName,
                          Set<String> analyzedPaths,
                          Set<String> declaredFetchPlanPaths,
                          PathComparisonResult comparisonResult) {
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass is null");
        this.methodName = Objects.requireNonNull(methodName, "methodName is null");
        this.rootParamName = Objects.requireNonNull(rootParamName, "rootParamName is null");
        this.rootType = null;
        this.fetchPlanName = fetchPlanName;
        this.analyzedPaths = copy(analyzedPaths);
        this.declaredFetchPlanPaths = copy(declaredFetchPlanPaths);
        this.comparisonResult = Objects.requireNonNull(comparisonResult, "comparisonResult is null");

        this.target = new AnalysisTarget(targetClass, methodName, rootParamName, null);
        this.canonicalPaths = copyList(reportPathsFrom(analyzedPaths));
        this.unsupportedConstructs = List.of();
        this.warnings = List.of();
        this.analysisLimits = Set.of();
    }

    public AnalysisTarget getTarget() {
        return target;
    }

    public List<ReportPath> getCanonicalPaths() {
        return canonicalPaths;
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

    public String getTargetClass() {
        return targetClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getRootParamName() {
        return rootParamName;
    }

    public String getRootType() {
        return rootType;
    }

    public String getFetchPlanName() {
        return fetchPlanName;
    }

    public Set<String> getAnalyzedPaths() {
        return analyzedPaths;
    }

    public Set<String> getDeclaredFetchPlanPaths() {
        return declaredFetchPlanPaths;
    }

    public PathComparisonResult getComparisonResult() {
        return comparisonResult;
    }

    private Set<String> copy(Set<String> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private <T> List<T> copyList(List<T> source) {
        Objects.requireNonNull(source, "source is null");
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    private Set<String> pathsFrom(List<ReportPath> paths) {
        Set<String> result = new LinkedHashSet<>();
        if (paths == null) {
            return result;
        }
        for (ReportPath path : paths) {
            result.add(path.getPath());
        }
        return result;
    }

    private List<ReportPath> reportPathsFrom(Set<String> paths) {
        List<ReportPath> result = new ArrayList<>();
        for (String path : new java.util.TreeSet<>(paths)) {
            result.add(new ReportPath(path, ReportConfidence.ANALYZED, List.of()));
        }
        return result;
    }
}
