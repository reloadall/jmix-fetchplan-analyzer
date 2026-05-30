package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ReportPath {

    private final String path;
    private final ReportConfidence confidence;
    private final List<ReportEvidence> evidence;

    public ReportPath(String path,
                      ReportConfidence confidence,
                      List<ReportEvidence> evidence) {
        this.path = Objects.requireNonNull(path, "path is null");
        this.confidence = Objects.requireNonNull(confidence, "confidence is null");
        this.evidence = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(evidence, "evidence is null")));
    }

    public String getPath() {
        return path;
    }

    public ReportConfidence getConfidence() {
        return confidence;
    }

    public List<ReportEvidence> getEvidence() {
        return evidence;
    }
}