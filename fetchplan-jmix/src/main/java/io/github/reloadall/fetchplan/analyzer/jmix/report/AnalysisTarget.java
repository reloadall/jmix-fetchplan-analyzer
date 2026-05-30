package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Objects;

public class AnalysisTarget {

    private final String className;
    private final String methodName;
    private final String rootParameterName;
    private final String rootType;

    public AnalysisTarget(String className,
                          String methodName,
                          String rootParameterName,
                          String rootType) {
        this.className = Objects.requireNonNull(className, "className is null");
        this.methodName = Objects.requireNonNull(methodName, "methodName is null");
        this.rootParameterName = Objects.requireNonNull(rootParameterName, "rootParameterName is null");
        this.rootType = rootType;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getRootParameterName() {
        return rootParameterName;
    }

    public String getRootType() {
        return rootType;
    }
}