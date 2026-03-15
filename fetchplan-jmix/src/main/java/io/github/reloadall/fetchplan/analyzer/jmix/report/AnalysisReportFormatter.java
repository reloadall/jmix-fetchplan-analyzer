package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component("fpa_AnalysisReportFormatter")
public class AnalysisReportFormatter {

    public String format(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("Method: ")
                .append(report.getTargetClass())
                .append(".")
                .append(report.getMethodName())
                .append("\n");

        sb.append("Root param: ")
                .append(report.getRootParamName())
                .append("\n");

        if (report.getFetchPlanName() != null && !report.getFetchPlanName().isBlank()) {
            sb.append("FetchPlan: ")
                    .append(report.getFetchPlanName())
                    .append("\n");
        }

        sb.append("\n");
        appendSection(sb, "Analyzed", report.getAnalyzedPaths());
        appendSection(sb, "Declared", report.getDeclaredFetchPlanPaths());
        appendSection(sb, "Covered", report.getComparisonResult().getMatchedPaths());
        appendSection(sb, "Missing", report.getComparisonResult().getMissingPaths());
        appendSection(sb, "Extra", report.getComparisonResult().getExtraPaths());
        appendSection(sb, "Uncertain", report.getComparisonResult().getUncertainPaths());

        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, Set<String> values) {
        sb.append(title).append(":\n");

        if (values == null || values.isEmpty()) {
            sb.append("- <none>\n\n");
            return;
        }

        for (String value : new TreeSet<>(values)) {
            sb.append("- ").append(value).append("\n");
        }
        sb.append("\n");
    }

}
