package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisReportContractTest {

    @Test
    void createsReportFromSuccessfulSingleMethodAnalysisResult() {
        AnalysisReport report = new AnalysisReportFactory().fromSingleMethodAnalysis(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                Set.of("customer.name", "type.code")
        );

        assertEquals("sample.DocumentService", report.getTarget().getClassName());
        assertEquals("inspect", report.getTarget().getMethodName());
        assertEquals("document", report.getTarget().getRootParameterName());
        assertEquals("sample.Document", report.getTarget().getRootType());
        assertEquals(List.of("customer.name", "type.code"), report.getCanonicalPaths().stream()
                .map(ReportPath::getPath)
                .toList());
        assertEquals(Set.of("customer.name", "type.code"), report.getAnalyzedPaths());
    }

    @Test
    void jsonOutputContainsTargetAndCanonicalPaths() {
        AnalysisReport report = sampleReport();

        String json = new AnalysisReportJsonRenderer().render(report);

        assertTrue(json.contains("\"target\": {"));
        assertTrue(json.contains("\"className\": \"sample.DocumentService\""));
        assertTrue(json.contains("\"methodName\": \"inspect\""));
        assertTrue(json.contains("\"rootParameterName\": \"document\""));
        assertTrue(json.contains("\"rootType\": \"sample.Document\""));
        assertTrue(json.contains("\"canonicalPaths\": ["));
        assertTrue(json.contains("\"path\": \"customer.name\""));
        assertTrue(json.contains("\"path\": \"type.code\""));
    }

    @Test
    void markdownOutputContainsTargetCanonicalPathsAndSafeEmptySections() {
        AnalysisReport report = sampleReport();

        String markdown = new AnalysisReportMarkdownRenderer().render(report);

        assertTrue(markdown.contains("## Target"));
        assertTrue(markdown.contains("- Class: `sample.DocumentService`"));
        assertTrue(markdown.contains("- Method: `inspect`"));
        assertTrue(markdown.contains("- Root parameter: `document`"));
        assertTrue(markdown.contains("- Root type: `sample.Document`"));
        assertTrue(markdown.contains("## Canonical Paths"));
        assertTrue(markdown.contains("- `customer.name` (ANALYZED)"));
        assertTrue(markdown.contains("- `type.code` (ANALYZED)"));
        assertTrue(markdown.contains("## Unsupported Constructs\n\nNone"));
        assertTrue(markdown.contains("## Warnings\n\nNone"));
    }

    @Test
    void outputOrderingIsDeterministic() {
        AnalysisReport report = new AnalysisReportFactory().fromSingleMethodAnalysis(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                Set.of("zeta.value", "alpha.value", "middle.value"),
                List.of(),
                List.of(),
                Set.of("limit-b", "limit-a")
        );

        AnalysisReportJsonRenderer jsonRenderer = new AnalysisReportJsonRenderer();
        AnalysisReportMarkdownRenderer markdownRenderer = new AnalysisReportMarkdownRenderer();

        assertEquals(jsonRenderer.render(report), jsonRenderer.render(report));
        assertEquals(markdownRenderer.render(report), markdownRenderer.render(report));

        String json = jsonRenderer.render(report);
        assertTrue(json.indexOf("alpha.value") < json.indexOf("middle.value"));
        assertTrue(json.indexOf("middle.value") < json.indexOf("zeta.value"));
        assertTrue(json.indexOf("limit-a") < json.indexOf("limit-b"));
    }

    private AnalysisReport sampleReport() {
        return new AnalysisReportFactory().fromSingleMethodAnalysis(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                Set.of("type.code", "customer.name")
        );
    }
}