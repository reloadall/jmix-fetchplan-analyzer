package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpectedPathComparisonServiceTest {

    private final ExpectedPathComparisonService service = new ExpectedPathComparisonService();
    private final ExpectedPathComparisonJsonRenderer jsonRenderer = new ExpectedPathComparisonJsonRenderer();
    private final ExpectedPathComparisonMarkdownRenderer markdownRenderer = new ExpectedPathComparisonMarkdownRenderer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exactMatchHasNoMissingOrExtraPaths() {
        ExpectedPathComparisonReport comparison = service.compare(report("lines.product.sku", "lines.quantity"), Set.of(
                "lines.product.sku",
                "lines.quantity"
        ));

        assertEquals(Set.of("lines.product.sku", "lines.quantity"), comparison.getMatchedPaths());
        assertTrue(comparison.getMissingPaths().isEmpty());
        assertTrue(comparison.getExtraPaths().isEmpty());
    }

    @Test
    void missingPathIsExpectedButNotFoundByAddon() {
        ExpectedPathComparisonReport comparison = service.compare(report("lines.product.sku"), Set.of(
                "lines.product.sku",
                "lines.quantity"
        ));

        assertEquals(Set.of("lines.quantity"), comparison.getMissingPaths());
        assertTrue(comparison.getExtraPaths().isEmpty());
    }

    @Test
    void extraPathIsFoundByAddonButNotExpected() {
        ExpectedPathComparisonReport comparison = service.compare(report("lines.product.sku", "lines.quantity"), Set.of(
                "lines.product.sku"
        ));

        assertEquals(Set.of("lines.quantity"), comparison.getExtraPaths());
        assertTrue(comparison.getMissingPaths().isEmpty());
    }

    @Test
    void duplicateAndBlankExpectedPathsAreIgnoredWithoutMutatingInput() {
        Set<String> expectedPaths = new LinkedHashSet<>(List.of(
                " lines.product.sku ",
                "",
                "lines.product.sku",
                "   ",
                "lines.quantity"
        ));
        Set<String> original = new LinkedHashSet<>(expectedPaths);

        ExpectedPathComparisonReport comparison = service.compare(report("lines.product.sku", "lines.quantity"), expectedPaths);

        assertEquals(Set.of("lines.product.sku", "lines.quantity"), comparison.getExpectedPaths());
        assertEquals(original, expectedPaths);
    }

    @Test
    void orderingIsDeterministic() {
        ExpectedPathComparisonReport comparison = service.compare(report("z.path", "a.path", "m.path"), Set.of(
                "m.path",
                "b.path",
                "a.path"
        ));

        assertEquals(List.of("a.path", "m.path", "z.path"), List.copyOf(comparison.getAddonCanonicalPaths()));
        assertEquals(List.of("a.path", "b.path", "m.path"), List.copyOf(comparison.getExpectedPaths()));
        assertEquals(List.of("a.path", "m.path"), List.copyOf(comparison.getMatchedPaths()));
        assertEquals(List.of("b.path"), List.copyOf(comparison.getMissingPaths()));
        assertEquals(List.of("z.path"), List.copyOf(comparison.getExtraPaths()));
    }

    @Test
    void jsonRendererProducesParseableDeterministicJson() throws Exception {
        ExpectedPathComparisonReport comparison = service.compare(report("z.path", "a.path"), Set.of("a.path", "b.path"));

        JsonNode root = objectMapper.readTree(jsonRenderer.render(comparison));

        assertEquals("ExampleService", root.path("target").path("className").asText());
        assertEquals("a.path", root.path("addonCanonicalPaths").get(0).asText());
        assertEquals("z.path", root.path("addonCanonicalPaths").get(1).asText());
        assertEquals("b.path", root.path("missingPaths").get(0).asText());
        assertEquals("z.path", root.path("extraPaths").get(0).asText());
    }

    @Test
    void jsonRendererEscapesSpecialValuesAndPreservesParsedValues() throws Exception {
        String className = "Example\"Service";
        String methodName = "inspect\\Method";
        String rootParameter = "doc\nument";
        String rootType = "Document\"Type";
        String addonMatched = "lines.\"sku";
        String addonExtra = "lines.path\\extra";
        String expectedMissing = "lines.missing\npath";
        String unsupportedExpression = "call(\"x\")\\next\nline";
        String warningMessage = "warn \"quoted\" \\slash\nline";
        String limit = "limit \"quoted\" \\slash\nline";

        ExpectedPathComparisonReport report = new ExpectedPathComparisonReport(
                new AnalysisTarget(className, methodName, rootParameter, rootType),
                new LinkedHashSet<>(List.of(addonMatched, addonExtra)),
                new LinkedHashSet<>(List.of(addonMatched, expectedMissing)),
                new LinkedHashSet<>(List.of(addonMatched)),
                new LinkedHashSet<>(List.of(expectedMissing)),
                new LinkedHashSet<>(List.of(addonExtra)),
                List.of(new ReportUnsupported("kind\"x", "file\\name", 42, unsupportedExpression, "message\ntext")),
                List.of(new ReportWarning("warning\"kind", "warn\\file", 7, warningMessage)),
                Set.of(limit)
        );

        JsonNode root = objectMapper.readTree(jsonRenderer.render(report));

        assertEquals(className, root.path("target").path("className").asText());
        assertEquals(methodName, root.path("target").path("methodName").asText());
        assertEquals(rootParameter, root.path("target").path("rootParameterName").asText());
        assertEquals(rootType, root.path("target").path("rootType").asText());
        assertArrayContains(root.path("addonCanonicalPaths"), addonMatched);
        assertArrayContains(root.path("expectedPaths"), expectedMissing);
        assertArrayContains(root.path("matchedPaths"), addonMatched);
        assertArrayContains(root.path("missingPaths"), expectedMissing);
        assertArrayContains(root.path("extraPaths"), addonExtra);
        assertEquals("kind\"x", root.path("unsupportedConstructs").get(0).path("kind").asText());
        assertEquals("file\\name", root.path("unsupportedConstructs").get(0).path("file").asText());
        assertEquals(42, root.path("unsupportedConstructs").get(0).path("line").asInt());
        assertEquals(unsupportedExpression, root.path("unsupportedConstructs").get(0).path("expression").asText());
        assertEquals("message\ntext", root.path("unsupportedConstructs").get(0).path("message").asText());
        assertEquals("warning\"kind", root.path("warnings").get(0).path("kind").asText());
        assertEquals("warn\\file", root.path("warnings").get(0).path("file").asText());
        assertEquals(7, root.path("warnings").get(0).path("line").asInt());
        assertEquals(warningMessage, root.path("warnings").get(0).path("message").asText());
        assertEquals(limit, root.path("analysisLimits").get(0).asText());
    }

    @Test
    void jsonAndMarkdownRenderNullableFields() throws Exception {
        ExpectedPathComparisonReport report = new ExpectedPathComparisonReport(
                new AnalysisTarget("ExampleService", "inspect", "document", null),
                Set.of("a.path"),
                Set.of("b.path"),
                Set.of(),
                Set.of("b.path"),
                Set.of("a.path"),
                List.of(new ReportUnsupported("unsupported", null, null, null, "unsupported message")),
                List.of(new ReportWarning("warning", null, null, "warning message")),
                Set.of()
        );

        JsonNode root = objectMapper.readTree(jsonRenderer.render(report));
        assertTrue(root.path("target").path("rootType").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("file").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("line").isNull());
        assertTrue(root.path("unsupportedConstructs").get(0).path("expression").isNull());
        assertTrue(root.path("warnings").get(0).path("file").isNull());
        assertTrue(root.path("warnings").get(0).path("line").isNull());

        String markdown = markdownRenderer.render(report);
        assertTrue(markdown.contains("- Root type: None"));
        assertTrue(markdown.contains("- **unsupported**: unsupported message"));
        assertTrue(markdown.contains("- **warning**: warning message"));
    }

    @Test
    void markdownRendererUsesNoneForEmptySections() {
        ExpectedPathComparisonReport comparison = service.compare(report("lines.product.sku"), Set.of("lines.product.sku"));

        String markdown = markdownRenderer.render(comparison);

        assertTrue(markdown.contains("## Missing Paths\n\nNone"));
        assertTrue(markdown.contains("## Extra Paths\n\nNone"));
        assertTrue(markdown.contains("## Unsupported Constructs\n\nNone"));
        assertTrue(markdown.contains("## Warnings\n\nNone"));
        assertTrue(markdown.contains("## Analysis Limits\n\nNone"));
    }

    @Test
    void cliParsesInlineExpectedPaths() {
        Set<String> paths = SingleMethodAnalysisReportCli.parseExpectedPaths(" a.path, b.path;\nc.path\r\n ; a.path ");

        assertEquals(List.of("a.path", "b.path", "c.path"), List.copyOf(paths));
    }

    @Test
    void cliReadsExpectedPathsFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("expected-paths.txt");
        Files.writeString(file, "# comment\n\n lines.product.sku \nlines.quantity\n", StandardCharsets.UTF_8);

        Set<String> paths = SingleMethodAnalysisReportCli.readExpectedPathsFile(file);

        assertEquals(List.of("lines.product.sku", "lines.quantity"), List.copyOf(paths));
        assertFalse(paths.contains("# comment"));
    }

    @Test
    void cliCompareTrueWithoutExpectedPathsFailsClearly() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                SingleMethodAnalysisReportCli.collectExpectedPaths(null, null, "true")
        );

        assertTrue(error.getMessage().contains("fetchplan.compare=true requires fetchplan.expectedPaths or fetchplan.expectedPathsFile"));
    }

    @Test
    void cliCombinesInlineAndFileExpectedPathsWithDeduplication(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("expected-paths.txt");
        Files.writeString(file, "# comment\n b.path \n\na.path\nc.path\n", StandardCharsets.UTF_8);

        Set<String> paths = SingleMethodAnalysisReportCli.collectExpectedPaths("a.path; d.path", file.toString(), "false");

        assertEquals(List.of("a.path", "d.path", "b.path", "c.path"), List.copyOf(paths));
    }

    @Test
    void cliMissingExpectedPathsFileProducesIOException(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.txt");

        assertThrows(java.io.IOException.class, () ->
                SingleMethodAnalysisReportCli.collectExpectedPaths(null, missing.toString(), null)
        );
    }

    @Test
    void compareFalseDoesNotSuppressComparisonWhenExpectedPathsAreSupplied() throws Exception {
        Set<String> expectedPaths = SingleMethodAnalysisReportCli.collectExpectedPaths("a.path", null, "false");

        assertTrue(SingleMethodAnalysisReportCli.comparisonEnabled(expectedPaths));
    }

    @Test
    void reportDefensivelyCopiesAndExposesUnmodifiableCollections() {
        Set<String> addonCanonicalPaths = new LinkedHashSet<>(List.of("addon.path"));
        Set<String> expectedPaths = new LinkedHashSet<>(List.of("expected.path"));
        Set<String> matchedPaths = new LinkedHashSet<>(List.of("matched.path"));
        Set<String> missingPaths = new LinkedHashSet<>(List.of("missing.path"));
        Set<String> extraPaths = new LinkedHashSet<>(List.of("extra.path"));
        List<ReportUnsupported> unsupportedConstructs = new ArrayList<>(List.of(
                new ReportUnsupported("unsupported", "file", 1, "expr", "message")
        ));
        List<ReportWarning> warnings = new ArrayList<>(List.of(
                new ReportWarning("warning", "file", 2, "message")
        ));
        Set<String> analysisLimits = new LinkedHashSet<>(List.of("limit"));

        ExpectedPathComparisonReport report = new ExpectedPathComparisonReport(
                new AnalysisTarget("ExampleService", "inspect", "document", "Document"),
                addonCanonicalPaths,
                expectedPaths,
                matchedPaths,
                missingPaths,
                extraPaths,
                unsupportedConstructs,
                warnings,
                analysisLimits
        );

        addonCanonicalPaths.add("mutated");
        expectedPaths.add("mutated");
        matchedPaths.add("mutated");
        missingPaths.add("mutated");
        extraPaths.add("mutated");
        unsupportedConstructs.clear();
        warnings.clear();
        analysisLimits.add("mutated");

        assertEquals(Set.of("addon.path"), report.getAddonCanonicalPaths());
        assertEquals(Set.of("expected.path"), report.getExpectedPaths());
        assertEquals(Set.of("matched.path"), report.getMatchedPaths());
        assertEquals(Set.of("missing.path"), report.getMissingPaths());
        assertEquals(Set.of("extra.path"), report.getExtraPaths());
        assertEquals(1, report.getUnsupportedConstructs().size());
        assertEquals(1, report.getWarnings().size());
        assertEquals(Set.of("limit"), report.getAnalysisLimits());

        assertThrows(UnsupportedOperationException.class, () -> report.getAddonCanonicalPaths().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> report.getExpectedPaths().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> report.getMatchedPaths().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> report.getMissingPaths().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> report.getExtraPaths().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> report.getUnsupportedConstructs().add(
                new ReportUnsupported("x", null, null, null, "x")
        ));
        assertThrows(UnsupportedOperationException.class, () -> report.getWarnings().add(
                new ReportWarning("x", null, null, "x")
        ));
        assertThrows(UnsupportedOperationException.class, () -> report.getAnalysisLimits().add("x"));
    }

    private AnalysisReport report(String... paths) {
        return new AnalysisReportFactory().fromSingleMethodAnalysis(
                new AnalysisTarget("ExampleService", "inspect", "document", "Document"),
                new LinkedHashSet<>(List.of(paths))
        );
    }

    private void assertArrayContains(JsonNode array, String expectedValue) {
        for (JsonNode item : array) {
            if (expectedValue.equals(item.asText())) {
                return;
            }
        }
        throw new AssertionError("Expected JSON array to contain: " + expectedValue + ", actual: " + array);
    }
}