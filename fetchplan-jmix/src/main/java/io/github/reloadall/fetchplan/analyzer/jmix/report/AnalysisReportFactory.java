package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component("fpa_AnalysisReportFactory")
public class AnalysisReportFactory {

    public AnalysisReport fromSingleMethodAnalysis(AnalysisTarget target, Set<String> canonicalPaths) {
        return fromSingleMethodAnalysis(target, canonicalPaths, List.of(), List.of(), Set.of());
    }

    public AnalysisReport fromSingleMethodAnalysis(AnalysisTarget target,
                                                   Set<String> canonicalPaths,
                                                   List<ReportUnsupported> unsupportedConstructs,
                                                   List<ReportWarning> warnings,
                                                   Set<String> analysisLimits) {
        List<ReportPath> reportPaths = new ArrayList<>();
        for (String path : new TreeSet<>(canonicalPaths)) {
            reportPaths.add(new ReportPath(path, ReportConfidence.ANALYZED, List.of()));
        }
        return new AnalysisReport(target, reportPaths, unsupportedConstructs, warnings, analysisLimits);
    }
}