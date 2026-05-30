package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component("fpa_ExpectedPathComparisonJsonRenderer")
public class ExpectedPathComparisonJsonRenderer {

    public String render(ExpectedPathComparisonReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        appendTarget(sb, report.getTarget());
        sb.append(",\n");
        appendStringArray(sb, "addonCanonicalPaths", report.getAddonCanonicalPaths(), 1);
        sb.append(",\n");
        appendStringArray(sb, "expectedPaths", report.getExpectedPaths(), 1);
        sb.append(",\n");
        appendStringArray(sb, "matchedPaths", report.getMatchedPaths(), 1);
        sb.append(",\n");
        appendStringArray(sb, "missingPaths", report.getMissingPaths(), 1);
        sb.append(",\n");
        appendStringArray(sb, "extraPaths", report.getExtraPaths(), 1);
        sb.append(",\n");
        appendUnsupported(sb, report.getUnsupportedConstructs());
        sb.append(",\n");
        appendWarnings(sb, report.getWarnings());
        sb.append(",\n");
        appendStringArray(sb, "analysisLimits", report.getAnalysisLimits(), 1);
        sb.append("\n}");
        return sb.toString();
    }

    private void appendTarget(StringBuilder sb, AnalysisTarget target) {
        sb.append("  \"target\": {\n");
        appendStringField(sb, "className", target.getClassName(), 2, true);
        appendStringField(sb, "methodName", target.getMethodName(), 2, true);
        appendStringField(sb, "rootParameterName", target.getRootParameterName(), 2, true);
        appendStringField(sb, "rootType", target.getRootType(), 2, false);
        sb.append("  }");
    }

    private void appendUnsupported(StringBuilder sb, List<ReportUnsupported> unsupported) {
        sb.append("  \"unsupportedConstructs\": [");
        List<ReportUnsupported> sorted = new ArrayList<>(unsupported);
        sorted.sort(Comparator.comparing(ReportUnsupported::getKind)
                .thenComparing(item -> safe(item.getFile()))
                .thenComparing(item -> item.getLine() == null ? -1 : item.getLine())
                .thenComparing(item -> safe(item.getExpression())));
        if (!sorted.isEmpty()) {
            sb.append("\n");
        }
        for (int i = 0; i < sorted.size(); i++) {
            ReportUnsupported item = sorted.get(i);
            sb.append("    {\n");
            appendStringField(sb, "kind", item.getKind(), 3, true);
            appendStringField(sb, "file", item.getFile(), 3, true);
            appendNumberField(sb, "line", item.getLine(), 3, true);
            appendStringField(sb, "expression", item.getExpression(), 3, true);
            appendStringField(sb, "message", item.getMessage(), 3, false);
            sb.append("\n    }");
            if (i < sorted.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]");
    }

    private void appendWarnings(StringBuilder sb, List<ReportWarning> warnings) {
        sb.append("  \"warnings\": [");
        List<ReportWarning> sorted = new ArrayList<>(warnings);
        sorted.sort(Comparator.comparing(ReportWarning::getKind)
                .thenComparing(item -> safe(item.getFile()))
                .thenComparing(item -> item.getLine() == null ? -1 : item.getLine()));
        if (!sorted.isEmpty()) {
            sb.append("\n");
        }
        for (int i = 0; i < sorted.size(); i++) {
            ReportWarning item = sorted.get(i);
            sb.append("    {\n");
            appendStringField(sb, "kind", item.getKind(), 3, true);
            appendStringField(sb, "file", item.getFile(), 3, true);
            appendNumberField(sb, "line", item.getLine(), 3, true);
            appendStringField(sb, "message", item.getMessage(), 3, false);
            sb.append("\n    }");
            if (i < sorted.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]");
    }

    private void appendStringArray(StringBuilder sb, String fieldName, Set<String> values, int indent) {
        indent(sb, indent).append("\"").append(fieldName).append("\": [");
        TreeSet<String> sorted = new TreeSet<>(values);
        int index = 0;
        for (String value : sorted) {
            if (index == 0) {
                sb.append("\n");
            }
            indent(sb, indent + 1).append("\"").append(escape(value)).append("\"");
            if (index < sorted.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            index++;
        }
        indent(sb, indent).append("]");
    }

    private void appendStringField(StringBuilder sb, String name, String value, int indent, boolean comma) {
        indent(sb, indent).append("\"").append(name).append("\": ");
        sb.append(value == null ? "null" : "\"" + escape(value) + "\"");
        if (comma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private void appendNumberField(StringBuilder sb, String name, Integer value, int indent, boolean comma) {
        indent(sb, indent).append("\"").append(name).append("\": ").append(value == null ? "null" : value);
        if (comma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private StringBuilder indent(StringBuilder sb, int level) {
        return sb.append("  ".repeat(level));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}