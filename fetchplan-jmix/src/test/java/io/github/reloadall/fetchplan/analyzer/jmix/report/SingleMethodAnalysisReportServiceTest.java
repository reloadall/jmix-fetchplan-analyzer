package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AstPathEngine;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.CastExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.CollectionGetExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ConditionalExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.EnclosedExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.InterprocMethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.MapMethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.MethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.NameExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.PassThroughMethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.payload.StatementsPayloadHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.PassThroughMethodPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.UnknownBreakPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ExpressionStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ForEachStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.IfStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ReturnStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKeyFactory;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocArgumentBinder;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlanner;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocReturnResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.SpringBeanImplementationResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentWorker;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SingleMethodAnalysisReportServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rendersSuccessfulJsonReportForKnownScenarioMethod() throws Exception {
        SingleMethodAnalysisReportService service = createService();

        String json = service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                Document.class.getName(),
                "json"
        );

        JsonNode root = objectMapper.readTree(json);
        assertEquals(DocumentScenarioService.class.getName(), root.path("target").path("className").asText());
        assertEquals("inspectFirstLine", root.path("target").path("methodName").asText());
        assertEquals("document", root.path("target").path("rootParameterName").asText());
        assertEquals(Document.class.getName(), root.path("target").path("rootType").asText());
        assertEquals("lines.product.sku", root.path("canonicalPaths").get(0).path("path").asText());
        assertEquals("lines.quantity", root.path("canonicalPaths").get(1).path("path").asText());
    }

    @Test
    void rendersSuccessfulMarkdownReportForKnownScenarioMethod() {
        SingleMethodAnalysisReportService service = createService();

        String markdown = service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                Document.class.getName(),
                "markdown"
        );

        assertTrue(markdown.contains("# Analysis Report"));
        assertTrue(markdown.contains("- Class: `" + DocumentScenarioService.class.getName() + "`"));
        assertTrue(markdown.contains("- Method: `inspectFirstLine`"));
        assertTrue(markdown.contains("- `lines.product.sku` (ANALYZED)"));
        assertTrue(markdown.contains("- `lines.quantity` (ANALYZED)"));
    }

    @Test
    void infersRootTypeWhenNotProvided() throws Exception {
        SingleMethodAnalysisReportService service = createService();

        String json = service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                null,
                null
        );

        JsonNode root = objectMapper.readTree(json);
        assertEquals("Document", root.path("target").path("rootType").asText());
    }

    @Test
    void missingRequiredArgumentProducesClearError() {
        SingleMethodAnalysisReportService service = createService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                " ",
                Document.class.getName(),
                "json"
        ));

        assertTrue(error.getMessage().contains("Missing required argument: rootParameterName"));
    }

    @Test
    void invalidFormatProducesClearError() {
        SingleMethodAnalysisReportService service = createService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                Document.class.getName(),
                "xml"
        ));

        assertTrue(error.getMessage().contains("Unsupported report format: xml"));
        assertTrue(error.getMessage().contains("Supported formats: json, markdown"));
    }

    @Test
    void methodOrRootParameterMismatchProducesClearResolverError() {
        SingleMethodAnalysisReportService service = createService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "missingDocument",
                Document.class.getName(),
                "json"
        ));

        assertTrue(error.getMessage().contains("Method not found"));
        assertTrue(error.getMessage().contains("rootParamName=missingDocument"));
    }

    @Test
    void outputIsDeterministicForSameInput() {
        SingleMethodAnalysisReportService service = createService();

        String first = service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                Document.class.getName(),
                "json"
        );
        String second = service.render(
                DocumentScenarioService.class.getName(),
                "inspectFirstLine",
                "document",
                Document.class.getName(),
                "json"
        );

        assertEquals(first, second);
    }

    private SingleMethodAnalysisReportService createService() {
        Path sourceRoot = Path.of("..", "fetchplan-jmix-test-scenarios", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));
        SourceAnalysisCache sourceAnalysisCache = new SourceAnalysisCache(sourceRootsResolver);
        AnalysisTrace analysisTrace = new AnalysisTrace();

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(DocumentWorker.class)).thenReturn((Map) Map.of());

        SpringBeanImplementationResolver springBeanImplementationResolver = new SpringBeanImplementationResolver(applicationContext, analysisTrace);
        InterprocMethodResolver interprocMethodResolver = new InterprocMethodResolver(
                springBeanImplementationResolver,
                sourceAnalysisCache,
                analysisTrace
        );
        InterprocArgumentBinder interprocArgumentBinder = new InterprocArgumentBinder(analysisTrace);
        InterprocReturnResolver interprocReturnResolver = new InterprocReturnResolver(analysisTrace);
        InterprocCallPlanner interprocCallPlanner = new InterprocCallPlanner(
                interprocMethodResolver,
                interprocArgumentBinder,
                interprocReturnResolver,
                analysisTrace
        );

        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(
                new NameExpressionHandler(),
                new CollectionGetExpressionHandler(),
                new MapMethodCallExpressionHandler(),
                new PassThroughMethodCallExpressionHandler(new PassThroughMethodPolicy()),
                new ConditionalExpressionHandler(),
                new InterprocMethodCallExpressionHandler(
                        interprocMethodResolver,
                        interprocArgumentBinder,
                        interprocReturnResolver,
                        analysisTrace
                ),
                new MethodCallExpressionHandler(),
                new EnclosedExpressionHandler(),
                new CastExpressionHandler()
        ));

        AstPathEngine astPathEngine = new AstPathEngine(
                List.of(new StatementsPayloadHandler(List.of(
                        new ExpressionStatementHandler(new UnknownBreakPolicy(), interprocCallPlanner),
                        new IfStatementHandler(),
                        new ForEachStatementHandler(interprocMethodResolver),
                        new ReturnStatementHandler(new UnknownBreakPolicy())
                ))),
                new EngineContext(expressionResolver),
                new VisitedKeyFactory(),
                analysisTrace
        );

        return new SingleMethodAnalysisReportService(
                new SourceMethodResolver(sourceAnalysisCache),
                astPathEngine,
                new RawTreeNormalizer(),
                new PathTreeFlattener(),
                new AnalysisReportFactory(),
                new AnalysisReportJsonRenderer(),
                new AnalysisReportMarkdownRenderer()
        );
    }
}