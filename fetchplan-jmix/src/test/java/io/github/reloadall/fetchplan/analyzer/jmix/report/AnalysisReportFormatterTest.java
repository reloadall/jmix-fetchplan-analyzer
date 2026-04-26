package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.compare.DeclaredPathBreakdown;
import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparisonResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisReportFormatterTest {

    @Test
    void rendersDeclaredNotConfirmedBreakdownSection() {
        AnalysisReport report = new AnalysisReport(
                "sample.Service",
                "inspect",
                "document",
                "sample-plan",
                Set.of("agreement.sides.counterparty.name"),
                Set.of("agreement", "agreement.sides", "agreement.sides.counterparty", "agreement.sides.counterparty.name", "agreement.sides.counterparty.code"),
                new PathComparisonResult(
                        Set.of("agreement.sides.counterparty.name"),
                        Set.of(),
                        Set.of("agreement.sides.counterparty.code"),
                        Set.of(),
                        new DeclaredPathBreakdown(
                                Set.of("agreement", "agreement.sides", "agreement.sides.counterparty"),
                                Set.of(),
                                Set.of("agreement.sides.counterparty.code"),
                                Set.of()
                        )
                )
        );

        String text = new AnalysisReportFormatter().format(report);

        assertTrue(text.contains("Declared not confirmed breakdown:"));
        assertTrue(text.contains("Possible analyzer gap:"));
        assertTrue(text.contains("agreement.sides.counterparty.code"));
        assertTrue(text.contains("Structural/container paths:"));
    }
}