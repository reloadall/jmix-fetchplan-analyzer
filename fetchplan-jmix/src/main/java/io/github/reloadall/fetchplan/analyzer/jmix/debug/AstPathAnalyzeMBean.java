package io.github.reloadall.fetchplan.analyzer.jmix.debug;

import java.util.Objects;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparator;
import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparisonResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AstPathEngine;
import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanExtractor;
import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanPathSet;
import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeUncertaintyExtractor;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTree;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.report.AnalysisReport;
import io.github.reloadall.fetchplan.analyzer.jmix.report.AnalysisReportFormatter;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.jmix.core.FetchPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component("fpa_AstPathAnalyzeMBean")
@ManagedResource(
        description = "Bin for manual testing of AST path analyzer",
        objectName = "io.github.reloadall.fetchplan.analyzer.managed:type=AstPathAnalyzeMBean")
public class AstPathAnalyzeMBean {

    private final SourceMethodResolver sourceMethodResolver;
    private final AstPathEngine astPathEngine;
    private final RawTreePrinter rawTreePrinter;
    private final RawTreeNormalizer rawTreeNormalizer;
    private final PathTreePrinter pathTreePrinter;
    private final PathTreeFlattener pathTreeFlattener;
    private final FetchPlanResolver fetchPlanResolver;
    private final FetchPlanExtractor fetchPlanExtractor;
    private final PathComparator pathComparator;
    private final RawTreeUncertaintyExtractor rawTreeUncertaintyExtractor;
    private final AnalysisReportFormatter analysisReportFormatter;
    private final AnalysisTrace analysisTrace;

    @Autowired
    public AstPathAnalyzeMBean(SourceMethodResolver sourceMethodResolver,
                               AstPathEngine astPathEngine,
                               RawTreePrinter rawTreePrinter,
                               RawTreeNormalizer rawTreeNormalizer,
                               PathTreePrinter pathTreePrinter,
                               PathTreeFlattener pathTreeFlattener,
                               FetchPlanResolver fetchPlanResolver,
                               FetchPlanExtractor fetchPlanExtractor,
                               PathComparator pathComparator,
                               RawTreeUncertaintyExtractor rawTreeUncertaintyExtractor,
                               AnalysisReportFormatter analysisReportFormatter,
                               AnalysisTrace analysisTrace) {
        this.sourceMethodResolver = sourceMethodResolver;
        this.astPathEngine = astPathEngine;
        this.rawTreePrinter = rawTreePrinter;
        this.rawTreeNormalizer = rawTreeNormalizer;
        this.pathTreePrinter = pathTreePrinter;
        this.pathTreeFlattener = pathTreeFlattener;
        this.fetchPlanResolver = fetchPlanResolver;
        this.fetchPlanExtractor = fetchPlanExtractor;
        this.pathComparator = pathComparator;
        this.rawTreeUncertaintyExtractor = rawTreeUncertaintyExtractor;
        this.analysisReportFormatter = analysisReportFormatter;
        this.analysisTrace = analysisTrace;
    }

    @ManagedOperation(description = "Run the AST parser and return trace")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "targetClass", description = "Full class name"),
            @ManagedOperationParameter(name = "methodName", description = "Method name"),
            @ManagedOperationParameter(name = "rootParamName", description = "Root parameter name"),
            @ManagedOperationParameter(name = "rootParamType", description = "Full name of the root parameter type")
    })
    public String analyzeTrace(String targetClass,
                               String methodName,
                               String rootParamName,
                               String rootParamType) {
        analysisTrace.start(targetClass + "." + methodName);
        try {
            analysisTrace.log("ROOT PARAM: " + rootParamName + " : " + rootParamType);
            analyzeRawTree(targetClass, methodName, rootParamName, rootParamType);
            return analysisTrace.dump();
        } finally {
            analysisTrace.clear();
        }
    }

    @ManagedOperation(description = "Run the AST parser and return RawTree")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "targetClass", description = "Full class name"),
            @ManagedOperationParameter(name = "methodName", description = "Method name"),
            @ManagedOperationParameter(name = "rootParamName", description = "Root parameter name"),
            @ManagedOperationParameter(name = "rootParamType", description = "Full name of the root parameter type")
    })
    public String analyzeRaw(String targetClass,
                             String methodName,
                             String rootParamName,
                             String rootParamType) {
        RawTree rawTree = analyzeRawTree(targetClass, methodName, rootParamName, rootParamType);
        return rawTreePrinter.print(rawTree);
    }

    @ManagedOperation(description = "Run the AST parser, normalize, and return the resulting paths")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "targetClass", description = "Full class name"),
            @ManagedOperationParameter(name = "methodName", description = "Method name"),
            @ManagedOperationParameter(name = "rootParamName", description = "Root parameter name"),
            @ManagedOperationParameter(name = "rootParamType", description = "Full name of the root parameter type")
    })
    public String analyzeNormalized(String targetClass,
                                    String methodName,
                                    String rootParamName,
                                    String rootParamType) {
        RawTree rawTree = analyzeRawTree(targetClass, methodName, rootParamName, rootParamType);
        PathTree pathTree = rawTreeNormalizer.normalize(rawTree);
        return pathTreePrinter.print(pathTree);
    }

    @ManagedOperation(description = "Run the AST analyzer and compare the result with the fetch plan")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "targetClass", description = "Full class name"),
            @ManagedOperationParameter(name = "methodName", description = "Method name"),
            @ManagedOperationParameter(name = "rootParamName", description = "Root parameter name"),
            @ManagedOperationParameter(name = "rootParamType", description = "Full name of the root parameter type"),
            @ManagedOperationParameter(name = "fetchPlanName", description = "Fetch plan name")
    })
    public String analyzeVsFetchPlan(String targetClass,
                                     String methodName,
                                     String rootParamName,
                                     String rootParamType,
                                     String fetchPlanName) throws ClassNotFoundException {

        Objects.requireNonNull(fetchPlanName, "fetchPlanName is null");

        RawTree rawTree = analyzeRawTree(targetClass, methodName, rootParamName, rootParamType);
        PathTree pathTree = rawTreeNormalizer.normalize(rawTree);
        Set<String> analyzedPaths = pathTreeFlattener.flatten(pathTree);

        Class<?> entityClass = Class.forName(rootParamType);
        FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanName);
        FetchPlanPathSet fetchPlanPathSet = fetchPlanExtractor.extract(fetchPlan);
        Set<String> declaredPaths = fetchPlanPathSet.getAllPaths();

        Set<String> uncertainPaths = rawTreeUncertaintyExtractor.extract(rawTree);

        PathComparisonResult comparisonResult = pathComparator.compare(
                analyzedPaths,
                fetchPlanPathSet,
                uncertainPaths
        );
        AnalysisReport report = new AnalysisReport(
                targetClass,
                methodName,
                rootParamName,
                fetchPlanName,
                analyzedPaths,
                declaredPaths,
                comparisonResult
        );

        return analysisReportFormatter.format(report);
    }

    private RawTree analyzeRawTree(String targetClass,
                                   String methodName,
                                   String rootParamName,
                                   String rootParamType) {
        Objects.requireNonNull(targetClass, "targetClass is null");
        Objects.requireNonNull(methodName, "methodName is null");
        Objects.requireNonNull(rootParamName, "rootParamName is null");
        Objects.requireNonNull(rootParamType, "rootParamType is null");

        MethodDeclaration method = sourceMethodResolver.resolve(
                targetClass,
                methodName,
                rootParamName,
                rootParamType
        );

        return astPathEngine.analyze(method, rootParamName);
    }

}
