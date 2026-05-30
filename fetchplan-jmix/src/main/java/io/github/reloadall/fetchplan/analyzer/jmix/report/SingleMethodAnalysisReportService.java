package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Objects;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AstPathEngine;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_SingleMethodAnalysisReportService")
public class SingleMethodAnalysisReportService {

    private final SourceMethodResolver sourceMethodResolver;
    private final AstPathEngine astPathEngine;
    private final RawTreeNormalizer rawTreeNormalizer;
    private final PathTreeFlattener pathTreeFlattener;
    private final AnalysisReportFactory analysisReportFactory;
    private final AnalysisReportJsonRenderer jsonRenderer;
    private final AnalysisReportMarkdownRenderer markdownRenderer;

    @Autowired
    public SingleMethodAnalysisReportService(SourceMethodResolver sourceMethodResolver,
                                             AstPathEngine astPathEngine,
                                             RawTreeNormalizer rawTreeNormalizer,
                                             PathTreeFlattener pathTreeFlattener,
                                             AnalysisReportFactory analysisReportFactory,
                                             AnalysisReportJsonRenderer jsonRenderer,
                                             AnalysisReportMarkdownRenderer markdownRenderer) {
        this.sourceMethodResolver = Objects.requireNonNull(sourceMethodResolver, "sourceMethodResolver is null");
        this.astPathEngine = Objects.requireNonNull(astPathEngine, "astPathEngine is null");
        this.rawTreeNormalizer = Objects.requireNonNull(rawTreeNormalizer, "rawTreeNormalizer is null");
        this.pathTreeFlattener = Objects.requireNonNull(pathTreeFlattener, "pathTreeFlattener is null");
        this.analysisReportFactory = Objects.requireNonNull(analysisReportFactory, "analysisReportFactory is null");
        this.jsonRenderer = Objects.requireNonNull(jsonRenderer, "jsonRenderer is null");
        this.markdownRenderer = Objects.requireNonNull(markdownRenderer, "markdownRenderer is null");
    }

    public AnalysisReport analyze(String targetClassName,
                                  String methodName,
                                  String rootParameterName,
                                  String rootType) {
        requireText(targetClassName, "targetClassName");
        requireText(methodName, "methodName");
        requireText(rootParameterName, "rootParameterName");

        String normalizedTargetClassName = targetClassName.trim();
        String normalizedMethodName = methodName.trim();
        String normalizedRootParameterName = rootParameterName.trim();

        MethodDeclaration method;
        String resolvedRootType = blankToNull(rootType);
        if (resolvedRootType == null) {
            method = sourceMethodResolver.resolve(normalizedTargetClassName, normalizedMethodName, normalizedRootParameterName);
            resolvedRootType = method.getParameterByName(normalizedRootParameterName)
                    .map(parameter -> parameter.getType().asString())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Root parameter not found after method resolution: " + normalizedRootParameterName
                    ));
        } else {
            method = sourceMethodResolver.resolve(normalizedTargetClassName, normalizedMethodName, normalizedRootParameterName, resolvedRootType);
        }

        RawTree rawTree = astPathEngine.analyze(method, normalizedRootParameterName);
        Set<String> canonicalPaths = pathTreeFlattener.flatten(rawTreeNormalizer.normalize(rawTree));

        return analysisReportFactory.fromSingleMethodAnalysis(
                new AnalysisTarget(normalizedTargetClassName, normalizedMethodName, normalizedRootParameterName, resolvedRootType),
                canonicalPaths
        );
    }

    public String render(String targetClassName,
                         String methodName,
                         String rootParameterName,
                         String rootType,
                         String format) {
        AnalysisReport report = analyze(targetClassName, methodName, rootParameterName, rootType);
        return render(report, format);
    }

    public String render(AnalysisReport report, String format) {
        String normalizedFormat = blankToNull(format);
        if (normalizedFormat == null || "json".equalsIgnoreCase(normalizedFormat)) {
            return jsonRenderer.render(report);
        }
        if ("markdown".equalsIgnoreCase(normalizedFormat)) {
            return markdownRenderer.render(report);
        }
        throw new IllegalArgumentException("Unsupported report format: " + format + ". Supported formats: json, markdown");
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + name);
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}