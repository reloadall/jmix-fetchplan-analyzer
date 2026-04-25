package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
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
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.junit.jupiter.api.Test;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ContractWorker;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.CustomerWorker;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentWorker;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AstPathEngineForEachWorkerFanOutTest {

    @Test
    void fansOutForeachOverInjectedWorkersToAllResolvedImplementations() {
        Path sourceRoot = Path.of("..", "fetchplan-jmix-test-scenarios", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));
        SourceAnalysisCache sourceAnalysisCache = new SourceAnalysisCache(sourceRootsResolver);
        AnalysisTrace analysisTrace = new AnalysisTrace();

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(DocumentWorker.class))
                .thenReturn((Map) Map.of(
                        "contractWorker", new ContractWorker(),
                        "customerWorker", new CustomerWorker()
                ));

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

        AstPathEngine engine = new AstPathEngine(
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

        MethodDeclaration method = new SourceMethodResolver(sourceAnalysisCache)
                .resolve(DocumentScenarioService.class.getName(), "inspectDocumentWithWorkers", "document", Document.class.getName());

        RawTree rawTree = engine.analyze(method, "document");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("contract.number", "contract.customer.name"), paths);
    }
}