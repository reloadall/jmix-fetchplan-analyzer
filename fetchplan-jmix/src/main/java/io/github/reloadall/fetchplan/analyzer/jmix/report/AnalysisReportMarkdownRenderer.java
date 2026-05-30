package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component("fpa_AnalysisReportMarkdownRenderer")
public class AnalysisReportMarkdownRenderer {

    public String render(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Analysis Report\n\n");
        appendTarget(sb, report.getTarget());
        appendCanonicalPaths(sb, report.getCanonicalPaths());
        appendUnsupported(sb, report.getUnsupportedConstructs());
        appendWarnings(sb, report.getWarnings());
        appendAnalysisLimits(sb, report.getAnalysisLimits());
        return sb.toString();
    }

    private void appendTarget(StringBuilder sb, AnalysisTarget target) {
        sb.append("## Target\n\n");
        sb.append("- Class: `").append(target.getClassName()).append("`\n");
        sb.append("- Method: `").append(target.getMethodName()).append("`\n");
        sb.append("- Root parameter: `").append(target.getRootParameterName()).append("`\n");
        sb.append("- Root type: ").append(target.getRootType() == null ? "None" : "`" + target.getRootType() + "`").append("\n\n");
    }

    private void appendCanonicalPaths(StringBuilder sb, List<ReportPath> paths) {
        sb.append("## Canonical Paths\n\n");
        if (paths.isEmpty()) {
            sb.append("None\n\n");
            return;
        }
        List<ReportPath> sorted = new ArrayList<>(paths);
        sorted.sort(Comparator.comparing(ReportPath::getPath));
        for (ReportPath path : sorted) {
            sb.append("- `").append(path.getPath()).append("` (").append(path.getConfidence()).append(")\n");
        }
        sb.append("\n");
    }

    private void appendUnsupported(StringBuilder sb, List<ReportUnsupported> unsupported) {
        sb.append("## Unsupported Constructs\n\n");
        if (unsupported.isEmpty()) {
            sb.append("None\n\n");
            return;
        }
        List<ReportUnsupported> sorted = new ArrayList<>(unsupported);
        sorted.sort(Comparator.comparing(ReportUnsupported::getKind)
                .thenComparing(item -> item.getFile() == null ? "" : item.getFile())
                .thenComparing(item -> item.getLine() == null ? -1 : item.getLine()));
        for (ReportUnsupported item : sorted) {
            sb.append("- **").append(item.getKind()).append("**: ").append(item.getMessage());
            appendLocation(sb, item.getFile(), item.getLine());
            if (item.getExpression() != null && !item.getExpression().isBlank()) {
                sb.append(" — `").append(item.getExpression()).append("`");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendWarnings(StringBuilder sb, List<ReportWarning> warnings) {
        sb.append("## Warnings\n\n");
        if (warnings.isEmpty()) {
            sb.append("None\n\n");
            return;
        }
        List<ReportWarning> sorted = new ArrayList<>(warnings);
        sorted.sort(Comparator.comparing(ReportWarning::getKind)
                .thenComparing(item -> item.getFile() == null ? "" : item.getFile())
                .thenComparing(item -> item.getLine() == null ? -1 : item.getLine()));
        for (ReportWarning item : sorted) {
            sb.append("- **").append(item.getKind()).append("**: ").append(item.getMessage());
            appendLocation(sb, item.getFile(), item.getLine());
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendAnalysisLimits(StringBuilder sb, Set<String> analysisLimits) {
        sb.append("## Analysis Limits\n\n");
        if (analysisLimits.isEmpty()) {
            sb.append("None\n");
            return;
        }
        for (String limit : new TreeSet<>(analysisLimits)) {
            sb.append("- ").append(limit).append("\n");
        }
    }

    private void appendLocation(StringBuilder sb, String file, Integer line) {
        if (file == null && line == null) {
            return;
        }
        sb.append(" (");
        if (file != null) {
            sb.append(file);
        }
        if (line != null) {
            sb.append(":").append(line);
        }
        sb.append(")");
    }
}