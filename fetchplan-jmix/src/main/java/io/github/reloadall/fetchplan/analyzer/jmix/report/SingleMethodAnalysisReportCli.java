package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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
            String rendered = service.render(
                    arguments.required("fetchplan.className"),
                    arguments.required("fetchplan.methodName"),
                    arguments.required("fetchplan.rootParam"),
                    arguments.optional("fetchplan.rootType"),
                    arguments.optionalOrDefault("fetchplan.format", "json")
            );

            String output = arguments.optional("fetchplan.output");
            if (output == null) {
                System.out.println(rendered);
            } else {
                writeOutput(Path.of(output), rendered);
            }
        }
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
    }
}