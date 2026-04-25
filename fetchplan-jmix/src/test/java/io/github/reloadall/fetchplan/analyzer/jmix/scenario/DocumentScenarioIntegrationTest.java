package io.github.reloadall.fetchplan.analyzer.jmix.scenario;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparator;
import io.github.reloadall.fetchplan.analyzer.jmix.compare.PathComparisonResult;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AstPathEngine;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanPathSet;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeUncertaintyExtractor;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.CollectionGetExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.CastExpressionHandler;
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
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.fixture.DocumentScenarioExpectedPaths;
import io.github.reloadall.fetchplan.analyzer.scenario.document.fixture.DocumentScenarioFetchPlanFixture;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.BaseLine;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.fixture.SyntheticLombokScenarioExpectedPaths;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.fixture.SyntheticLombokScenarioFetchPlanFixture;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service.SyntheticLombokScenarioService;
import org.junit.jupiter.api.Test;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentWorker;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ContractWorker;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.CustomerWorker;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentScenarioIntegrationTest {

    @Test
    void analyzesDocumentScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocument",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_LEAF_PATHS
        );
    }

    @Test
    void analyzesIfElseScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentBranch",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_BRANCH,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_BRANCH_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_BRANCH_LEAF_PATHS
        );
    }

    @Test
    void analyzesCollectionGetScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectFirstLine",
                DocumentScenarioExpectedPaths.INSPECT_FIRST_LINE,
                DocumentScenarioFetchPlanFixture.INSPECT_FIRST_LINE_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_FIRST_LINE_LEAF_PATHS
        );
    }

    @Test
    void analyzesThisCallScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithThisCall",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_THIS_CALL,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_THIS_CALL_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_THIS_CALL_LEAF_PATHS
        );
    }

    @Test
    void analyzesValueCallScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithValueCall",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_VALUE_CALL,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_VALUE_CALL_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_VALUE_CALL_LEAF_PATHS
        );
    }

    @Test
    void analyzesLocalAliasScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithLocalAlias",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_LOCAL_ALIAS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_LOCAL_ALIAS_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_LOCAL_ALIAS_LEAF_PATHS
        );
    }

    @Test
    void analyzesAliasChainScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithAliasChain",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_ALIAS_CHAIN,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_ALIAS_CHAIN_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_ALIAS_CHAIN_LEAF_PATHS
        );
    }

    @Test
    void analyzesCastScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithCast",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_CAST,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_CAST_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_CAST_LEAF_PATHS
        );
    }

    @Test
    void analyzesStreamMapScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithStreamMap",
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_STREAM_MAP,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_STREAM_MAP_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_STREAM_MAP_LEAF_PATHS
        );
    }

    @Test
    void analyzesUnknownBreakScenarioAndReportsUncertainty() {
        ScenarioResult result = analyzeScenario(
                "inspectDocumentWithUnknownBreak",
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_LEAF_PATHS
        );

        assertEquals(DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_PATHS, result.analyzedPaths());
        assertEquals(DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_UNCERTAIN, result.rawUncertainPaths());
        assertEquals(Set.of(), result.comparisonResult().getMatchedPaths());
        assertEquals(Set.of(), result.comparisonResult().getMissingPaths());
        assertEquals(Set.of(), result.comparisonResult().getExtraPaths());
        assertEquals(Set.of("<root>", "shippingAddress.city"), result.comparisonResult().getUncertainPaths());
    }

    @Test
    void analyzesWorkerCollectionScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithWorkers",
                DocumentScenarioService.class,
                "document",
                Document.class,
                DocumentScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_WORKERS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_WORKERS_ALL_PATHS,
                DocumentScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_WORKERS_LEAF_PATHS
        );
    }

    @Test
    void analyzesLombokSingleBeanScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithLombokServiceCall",
                SyntheticLombokScenarioService.class,
                "document",
                RootDocument.class,
                SyntheticLombokScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL_ALL_PATHS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL_LEAF_PATHS
        );
    }

    @Test
    void analyzesChainedCrossServiceReturnRebindingScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithChainedFinders",
                SyntheticLombokScenarioService.class,
                "document",
                RootDocument.class,
                SyntheticLombokScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_CHAINED_FINDERS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_CHAINED_FINDERS_ALL_PATHS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_CHAINED_FINDERS_LEAF_PATHS
        );
    }

    @Test
    void analyzesMultiOriginAgreementScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectDocumentWithMultiOriginAgreement",
                SyntheticLombokScenarioService.class,
                "document",
                RootDocument.class,
                SyntheticLombokScenarioExpectedPaths.INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT_ALL_PATHS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT_LEAF_PATHS
        );
    }

    @Test
    void analyzesHelperGuardedExplicitCastScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectLineWithTypeGuardAndCast",
                SyntheticLombokScenarioService.class,
                "line",
                BaseLine.class,
                SyntheticLombokScenarioExpectedPaths.INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST_ALL_PATHS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST_LEAF_PATHS
        );
    }

    @Test
    void analyzesNegativeHelperGuardedExplicitCastScenarioAndMatchesFixturePaths() {
        assertScenario(
                "inspectLineWithNegativeTypeGuardAndCast",
                SyntheticLombokScenarioService.class,
                "line",
                BaseLine.class,
                SyntheticLombokScenarioExpectedPaths.INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST_ALL_PATHS,
                SyntheticLombokScenarioFetchPlanFixture.INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST_LEAF_PATHS
        );
    }

    private void assertScenario(String methodName,
                                Set<String> expectedPaths,
                                Set<String> allFetchPlanPaths,
                                Set<String> leafFetchPlanPaths) {
        assertScenario(
                methodName,
                DocumentScenarioService.class,
                "document",
                Document.class,
                expectedPaths,
                allFetchPlanPaths,
                leafFetchPlanPaths
        );
    }

    private void assertScenario(String methodName,
                                Class<?> targetServiceClass,
                                String rootParameterName,
                                Class<?> rootParameterType,
                                Set<String> expectedPaths,
                                Set<String> allFetchPlanPaths,
                                Set<String> leafFetchPlanPaths) {
        ScenarioResult result = analyzeScenario(
                targetServiceClass,
                methodName,
                rootParameterName,
                rootParameterType,
                allFetchPlanPaths,
                leafFetchPlanPaths
        );

        assertEquals(expectedPaths, result.analyzedPaths(), result.trace());
        assertEquals(expectedPaths, result.comparisonResult().getMatchedPaths(), result.trace());
        assertEquals(Set.of(), result.comparisonResult().getMissingPaths(), result.trace());
        assertEquals(Set.of(), result.comparisonResult().getExtraPaths(), result.trace());
        assertEquals(Set.of(), result.comparisonResult().getUncertainPaths(), result.trace());
    }

    private ScenarioResult analyzeScenario(String methodName,
                                           Set<String> allFetchPlanPaths,
                                           Set<String> leafFetchPlanPaths) {
        return analyzeScenario(
                DocumentScenarioService.class,
                methodName,
                "document",
                Document.class,
                allFetchPlanPaths,
                leafFetchPlanPaths
        );
    }

    private ScenarioResult analyzeScenario(Class<?> targetServiceClass,
                                           String methodName,
                                           String rootParameterName,
                                           Class<?> rootParameterType,
                                           Set<String> allFetchPlanPaths,
                                           Set<String> leafFetchPlanPaths) {
        AnalysisTrace analysisTrace = new AnalysisTrace();

        Path scenarioSourceRoot = Path.of("..", "fetchplan-jmix-test-scenarios", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(java.util.List.of(scenarioSourceRoot));

        SourceMethodResolver sourceMethodResolver = new SourceMethodResolver(
                new SourceAnalysisCache(sourceRootsResolver)
        );

        SpringBeanImplementationResolver springBeanImplementationResolver = new SpringBeanImplementationResolver(
                mockWorkerAwareApplicationContext(),
                analysisTrace
        );
        InterprocMethodResolver interprocMethodResolver = new InterprocMethodResolver(
                springBeanImplementationResolver,
                new SourceAnalysisCache(sourceRootsResolver),
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

        ExpressionResolver expressionResolver = new ExpressionResolver(java.util.List.of(
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
        EngineContext engineContext = new EngineContext(expressionResolver);
        AstPathEngine astPathEngine = new AstPathEngine(
                java.util.List.of(new StatementsPayloadHandler(java.util.List.of(
                        new ExpressionStatementHandler(new UnknownBreakPolicy(), interprocCallPlanner),
                        new IfStatementHandler(),
                        new ForEachStatementHandler(interprocMethodResolver),
                        new ReturnStatementHandler(new UnknownBreakPolicy())
                ))),
                engineContext,
                new VisitedKeyFactory(),
                analysisTrace
        );

        RawTreeNormalizer rawTreeNormalizer = new RawTreeNormalizer();
        PathTreeFlattener pathTreeFlattener = new PathTreeFlattener();
        PathComparator pathComparator = new PathComparator();
        RawTreeUncertaintyExtractor rawTreeUncertaintyExtractor = new RawTreeUncertaintyExtractor();

        MethodDeclaration method = sourceMethodResolver.resolve(
                targetServiceClass.getName(),
                methodName,
                rootParameterName,
                rootParameterType.getName()
        );

        RawTree rawTree = astPathEngine.analyze(method, rootParameterName);
        Set<String> analyzedPaths = pathTreeFlattener.flatten(rawTreeNormalizer.normalize(rawTree));
        Set<String> uncertainPaths = rawTreeUncertaintyExtractor.extract(rawTree);

        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                allFetchPlanPaths,
                leafFetchPlanPaths
        );

        PathComparisonResult comparisonResult = pathComparator.compare(
                analyzedPaths,
                fetchPlanPathSet,
                uncertainPaths
        );

        return new ScenarioResult(analyzedPaths, uncertainPaths, comparisonResult, analysisTrace.dump());
    }

    private record ScenarioResult(Set<String> analyzedPaths,
                                  Set<String> rawUncertainPaths,
                                  PathComparisonResult comparisonResult,
                                  String trace) {
    }

    private ApplicationContext mockWorkerAwareApplicationContext() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(DocumentWorker.class)).thenReturn((Map) Map.of(
                "contractWorker", new ContractWorker(),
                "customerWorker", new CustomerWorker()
        ));
        return applicationContext;
    }
}