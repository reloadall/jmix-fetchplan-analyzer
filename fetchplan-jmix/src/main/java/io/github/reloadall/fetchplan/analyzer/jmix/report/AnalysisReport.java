package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparisonResult;

public class AnalysisReport {

    private final String targetClass;
    private final String methodName;
    private final String rootParamName;
    private final String fetchPlanName;
    private final Set<String> analyzedPaths;
    private final Set<String> declaredFetchPlanPaths;
    private final PathComparisonResult comparisonResult;

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
        this.fetchPlanName = fetchPlanName;
        this.analyzedPaths = copy(analyzedPaths);
        this.declaredFetchPlanPaths = copy(declaredFetchPlanPaths);
        this.comparisonResult = Objects.requireNonNull(comparisonResult, "comparisonResult is null");
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
}
