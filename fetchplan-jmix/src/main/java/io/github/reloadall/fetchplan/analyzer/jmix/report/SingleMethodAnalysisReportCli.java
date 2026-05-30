package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

public final class SingleMethodAnalysisReportCli {

    private SingleMethodAnalysisReportCli() {
    }

    public static void main(String[] args) throws IOException {
        CliArguments arguments = CliArguments.parse(args);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(SingleMethodAnalysisReportCliConfiguration.class);
            context.refresh();

            SingleMethodAnalysisReportService service = context.getBean(SingleMethodAnalysisReportService.class);
            String format = arguments.optionalOrDefault("fetchplan.format", "json");
            AnalysisReport report = service.analyze(
                    arguments.required("fetchplan.className"),
                    arguments.required("fetchplan.methodName"),
                    arguments.required("fetchplan.rootParam"),
                    arguments.optional("fetchplan.rootType")
            );
            String rendered;
            Set<String> expectedPaths = arguments.expectedPaths();
            if (!comparisonEnabled(expectedPaths)) {
                rendered = service.render(report, format);
            } else {
                ExpectedPathComparisonReport comparisonReport = context.getBean(ExpectedPathComparisonService.class)
                        .compare(report, expectedPaths);
                rendered = renderComparison(
                        comparisonReport,
                        format,
                        context.getBean(ExpectedPathComparisonJsonRenderer.class),
                        context.getBean(ExpectedPathComparisonMarkdownRenderer.class)
                );
            }

            String output = arguments.optional("fetchplan.output");
            if (output == null) {
                System.out.println(rendered);
            } else {
                writeOutput(Path.of(output), rendered);
            }
        }
    }

    private static String renderComparison(ExpectedPathComparisonReport report,
                                           String format,
                                           ExpectedPathComparisonJsonRenderer jsonRenderer,
                                           ExpectedPathComparisonMarkdownRenderer markdownRenderer) {
        String normalizedFormat = format == null || format.isBlank() ? "json" : format.trim();
        if ("json".equalsIgnoreCase(normalizedFormat)) {
            return jsonRenderer.render(report);
        }
        if ("markdown".equalsIgnoreCase(normalizedFormat)) {
            return markdownRenderer.render(report);
        }
        throw new IllegalArgumentException("Unsupported report format: " + format + ". Supported formats: json, markdown");
    }

    @Configuration
    @ComponentScan(
            basePackages = "io.github.reloadall.fetchplan.analyzer.jmix",
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.github\\.reloadall\\.fetchplan\\.analyzer\\.jmix\\.FetchPlanAnalyzerJmixConfiguration"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.github\\.reloadall\\.fetchplan\\.analyzer\\.jmix\\.debug\\.AstPathAnalyzeMBean"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.github\\.reloadall\\.fetchplan\\.analyzer\\.jmix\\.fetchplan\\..*"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "io\\.github\\.reloadall\\.fetchplan\\.analyzer\\.jmix\\.compare\\..*")
            }
    )
    @ConfigurationPropertiesScan(basePackages = "io.github.reloadall.fetchplan.analyzer.jmix")
    static class SingleMethodAnalysisReportCliConfiguration {
    }

    static Set<String> parseExpectedPaths(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String path : value.split("[,;\\r\\n]+")) {
            if (path == null || path.isBlank()) {
                continue;
            }
            result.add(path.trim());
        }
        return result;
    }

    static Set<String> readExpectedPathsFile(Path file) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }

    static Set<String> collectExpectedPaths(String inlineExpectedPaths,
                                            String expectedPathsFile,
                                            String compare) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(parseExpectedPaths(inlineExpectedPaths));

        if (expectedPathsFile != null && !expectedPathsFile.isBlank()) {
            result.addAll(readExpectedPathsFile(Path.of(expectedPathsFile.trim())));
        }

        if (result.isEmpty() && "true".equalsIgnoreCase(blankToNull(compare))) {
            throw new IllegalArgumentException(
                    "fetchplan.compare=true requires fetchplan.expectedPaths or fetchplan.expectedPathsFile"
            );
        }

        return result;
    }

    static boolean comparisonEnabled(Set<String> expectedPaths) {
        return expectedPaths != null && !expectedPaths.isEmpty();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void writeOutput(Path output, String rendered) throws IOException {
        Path normalizedOutput = output.toAbsolutePath().normalize();
        Path parent = normalizedOutput.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(normalizedOutput, rendered, StandardCharsets.UTF_8);
    }

    private static final class CliArguments {

        private final Map<String, String> values;

        private CliArguments(Map<String, String> values) {
            this.values = values;
        }

        private static CliArguments parse(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                int separator = arg.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalArgumentException("Invalid argument, expected key=value: " + arg);
                }
                values.put(arg.substring(0, separator), arg.substring(separator + 1));
            }
            return new CliArguments(values);
        }

        private String required(String name) {
            String value = optional(name);
            if (value == null) {
                throw new IllegalArgumentException("Missing required Gradle property: " + name);
            }
            return value;
        }

        private String optionalOrDefault(String name, String defaultValue) {
            String value = optional(name);
            return value == null ? defaultValue : value;
        }

        private String optional(String name) {
            String value = values.get(name);
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.trim();
        }

        private Set<String> expectedPaths() throws IOException {
            return collectExpectedPaths(
                    optional("fetchplan.expectedPaths"),
                    optional("fetchplan.expectedPathsFile"),
                    optional("fetchplan.compare")
            );
        }
    }
}