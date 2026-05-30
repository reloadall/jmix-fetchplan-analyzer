package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisReportContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void jsonOutputIsParseableAndContainsExpectedStructure() throws Exception {
        AnalysisReport report = sampleReport();

        JsonNode root = parseJson(report);

        assertEquals("sample.DocumentService", root.path("target").path("className").asText());
        assertEquals("inspect", root.path("target").path("methodName").asText());
        assertEquals("document", root.path("target").path("rootParameterName").asText());
        assertEquals("sample.Document", root.path("target").path("rootType").asText());
        assertEquals("customer.name", root.path("canonicalPaths").get(0).path("path").asText());
        assertEquals("type.code", root.path("canonicalPaths").get(1).path("path").asText());
        assertTrue(root.path("unsupportedConstructs").isArray());
        assertTrue(root.path("warnings").isArray());
        assertTrue(root.path("analysisLimits").isArray());
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
    void rendersNonEmptyEvidenceUnsupportedWarningsAndAnalysisLimits() throws Exception {
        AnalysisReport report = complexReport();

        JsonNode root = parseJson(report);
        JsonNode firstPath = root.path("canonicalPaths").get(0);
        assertEquals("customer.name", firstPath.path("path").asText());
        assertEquals("DocumentService.java", firstPath.path("evidence").get(0).path("file").asText());
        assertEquals(42, firstPath.path("evidence").get(0).path("line").asInt());
        assertEquals("document.getCustomer().getName()", firstPath.path("evidence").get(0).path("expression").asText());
        assertEquals("getter chain", firstPath.path("evidence").get(0).path("note").asText());
        assertEquals("STATIC_HELPER", root.path("unsupportedConstructs").get(0).path("kind").asText());
        assertEquals("UNCERTAIN_SOURCE", root.path("warnings").get(0).path("kind").asText());
        assertEquals("source-evidence-best-effort", root.path("analysisLimits").get(0).asText());

        String markdown = new AnalysisReportMarkdownRenderer().render(report);
        assertTrue(markdown.contains("- `customer.name` (ANALYZED)"));
        assertTrue(markdown.contains("- **STATIC_HELPER**: Static helper cannot be resolved (DocumentService.java:45) — `Helper.inspect(document)`"));
        assertTrue(markdown.contains("- **UNCERTAIN_SOURCE**: Source evidence is best-effort (DocumentService.java:43)"));
        assertTrue(markdown.contains("- source-evidence-best-effort"));
    }

    @Test
    void jsonEscapesSpecialCharactersAndPreservesValuesAfterParsing() throws Exception {
        String special = "quoted \"value\" with backslash \\ and newline\nend";
        AnalysisReport report = new AnalysisReport(
                new AnalysisTarget("sample.SpecialService", "inspect\"Special", "document", special),
                List.of(new ReportPath(
                        "customer.special",
                        ReportConfidence.ANALYZED,
                        List.of(new ReportEvidence(special, 7, special, special))
                )),
                List.of(new ReportUnsupported("SPECIAL", special, 8, special, special)),
                List.of(new ReportWarning("SPECIAL_WARNING", special, 9, special)),
                Set.of(special)
        );

        JsonNode root = parseJson(report);

        assertEquals("inspect\"Special", root.path("target").path("methodName").asText());
        assertEquals(special, root.path("target").path("rootType").asText());
        assertEquals(special, root.path("canonicalPaths").get(0).path("evidence").get(0).path("file").asText());
        assertEquals(special, root.path("canonicalPaths").get(0).path("evidence").get(0).path("expression").asText());
        assertEquals(special, root.path("canonicalPaths").get(0).path("evidence").get(0).path("note").asText());
        assertEquals(special, root.path("unsupportedConstructs").get(0).path("message").asText());
        assertEquals(special, root.path("warnings").get(0).path("message").asText());
        assertEquals(special, root.path("analysisLimits").get(0).asText());
    }

    @Test
    void nullableFieldsRenderSafely() throws Exception {
        AnalysisReport report = new AnalysisReport(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", null),
                List.of(new ReportPath(
                        "customer.name",
                        ReportConfidence.ANALYZED,
                        List.of(new ReportEvidence(null, null, "document.getCustomer().getName()", null))
                )),
                List.of(new ReportUnsupported("UNKNOWN", null, null, null, "Unsupported without location")),
                List.of(new ReportWarning("WARNING", null, null, "Warning without location")),
                Set.of()
        );

        JsonNode root = parseJson(report);

        assertTrue(root.path("target").path("rootType").isNull());
        JsonNode evidence = root.path("canonicalPaths").get(0).path("evidence").get(0);
        assertTrue(evidence.path("file").isNull());
        assertTrue(evidence.path("line").isNull());
        assertTrue(evidence.path("note").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("file").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("line").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("expression").isNull());
        assertTrue(root.path("warnings").get(0).path("file").isNull());
        assertTrue(root.path("warnings").get(0).path("line").isNull());

        String markdown = new AnalysisReportMarkdownRenderer().render(report);
        assertTrue(markdown.contains("- Root type: None"));
        assertTrue(markdown.contains("- **UNKNOWN**: Unsupported without location"));
        assertTrue(markdown.contains("- **WARNING**: Warning without location"));
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

    @Test
    void unsupportedWarningsAndLimitsAreSortedButEvidenceOrderIsPreserved() throws Exception {
        AnalysisReport report = new AnalysisReport(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                List.of(new ReportPath(
                        "customer.name",
                        ReportConfidence.ANALYZED,
                        List.of(
                                new ReportEvidence("z-source.java", 30, "zExpression", "second source occurrence kept first"),
                                new ReportEvidence("a-source.java", 10, "aExpression", "first source occurrence kept second")
                        )
                )),
                List.of(
                        new ReportUnsupported("Z_KIND", "z.java", 2, "z()", "z message"),
                        new ReportUnsupported("A_KIND", "a.java", 1, "a()", "a message")
                ),
                List.of(
                        new ReportWarning("Z_WARNING", "z.java", 2, "z warning"),
                        new ReportWarning("A_WARNING", "a.java", 1, "a warning")
                ),
                Set.of("limit-z", "limit-a")
        );

        JsonNode root = parseJson(report);

        assertEquals("A_KIND", root.path("unsupportedConstructs").get(0).path("kind").asText());
        assertEquals("Z_KIND", root.path("unsupportedConstructs").get(1).path("kind").asText());
        assertEquals("A_WARNING", root.path("warnings").get(0).path("kind").asText());
        assertEquals("Z_WARNING", root.path("warnings").get(1).path("kind").asText());
        assertEquals("limit-a", root.path("analysisLimits").get(0).asText());
        assertEquals("limit-z", root.path("analysisLimits").get(1).asText());

        JsonNode evidence = root.path("canonicalPaths").get(0).path("evidence");
        assertEquals("z-source.java", evidence.get(0).path("file").asText());
        assertEquals("a-source.java", evidence.get(1).path("file").asText());
    }

    @Test
    void reportCollectionsAreDefensivelyCopiedAndUnmodifiable() {
        List<ReportPath> paths = new ArrayList<>();
        paths.add(new ReportPath("customer.name", ReportConfidence.ANALYZED, List.of()));

        List<ReportUnsupported> unsupported = new ArrayList<>();
        unsupported.add(new ReportUnsupported("UNKNOWN", "a.java", 1, "unknown()", "unknown"));

        List<ReportWarning> warnings = new ArrayList<>();
        warnings.add(new ReportWarning("WARNING", "a.java", 1, "warning"));

        Set<String> limits = new LinkedHashSet<>();
        limits.add("limit-a");

        AnalysisReport report = new AnalysisReport(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                paths,
                unsupported,
                warnings,
                limits
        );

        paths.add(new ReportPath("type.code", ReportConfidence.ANALYZED, List.of()));
        unsupported.add(new ReportUnsupported("LATER", "b.java", 2, "later()", "later"));
        warnings.add(new ReportWarning("LATER_WARNING", "b.java", 2, "later"));
        limits.add("limit-b");

        assertEquals(1, report.getCanonicalPaths().size());
        assertEquals(1, report.getUnsupportedConstructs().size());
        assertEquals(1, report.getWarnings().size());
        assertEquals(Set.of("limit-a"), report.getAnalysisLimits());

        assertThrows(UnsupportedOperationException.class, () -> report.getCanonicalPaths().add(
                new ReportPath("another.path", ReportConfidence.ANALYZED, List.of())
        ));
        assertThrows(UnsupportedOperationException.class, () -> report.getUnsupportedConstructs().add(
                new ReportUnsupported("ANOTHER", null, null, null, "another")
        ));
        assertThrows(UnsupportedOperationException.class, () -> report.getWarnings().add(
                new ReportWarning("ANOTHER", null, null, "another")
        ));
        assertThrows(UnsupportedOperationException.class, () -> report.getAnalysisLimits().add("limit-c"));
    }

    @Test
    void reportPathEvidenceIsDefensivelyCopiedAndUnmodifiable() {
        List<ReportEvidence> evidence = new ArrayList<>();
        evidence.add(new ReportEvidence("first.java", 1, "first", null));

        ReportPath path = new ReportPath("customer.name", ReportConfidence.ANALYZED, evidence);

        evidence.add(new ReportEvidence("second.java", 2, "second", null));

        assertEquals(1, path.getEvidence().size());
        assertEquals("first.java", path.getEvidence().get(0).getFile());
        assertThrows(UnsupportedOperationException.class, () -> path.getEvidence().add(
                new ReportEvidence("third.java", 3, "third", null)
        ));
    }

    private AnalysisReport sampleReport() {
        return new AnalysisReportFactory().fromSingleMethodAnalysis(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                Set.of("type.code", "customer.name")
        );
    }

    private AnalysisReport complexReport() {
        return new AnalysisReport(
                new AnalysisTarget("sample.DocumentService", "inspect", "document", "sample.Document"),
                List.of(new ReportPath(
                        "customer.name",
                        ReportConfidence.ANALYZED,
                        List.of(new ReportEvidence(
                                "DocumentService.java",
                                42,
                                "document.getCustomer().getName()",
                                "getter chain"
                        ))
                )),
                List.of(new ReportUnsupported(
                        "STATIC_HELPER",
                        "DocumentService.java",
                        45,
                        "Helper.inspect(document)",
                        "Static helper cannot be resolved"
                )),
                List.of(new ReportWarning(
                        "UNCERTAIN_SOURCE",
                        "DocumentService.java",
                        43,
                        "Source evidence is best-effort"
                )),
                Set.of("source-evidence-best-effort")
        );
    }

    private JsonNode parseJson(AnalysisReport report) throws Exception {
        return objectMapper.readTree(new AnalysisReportJsonRenderer().render(report));
    }
}