package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.nio.file.Path;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
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
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ReturnRebindingFixtureService.FullChainAct;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ReturnRebindingFixtureService.GroupingAct;
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ReturnRebindingFixtureService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReturnRebindingCanonicalOutputTest {

    @Test
    void sameClassHelperReturnRebindingShouldKeepOnlyLeafCanonicalPath() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("sameClassHelperReturnRebinding")
        );
    }

    @Test
    void explicitThisCallReturnRebindingShouldKeepOnlyLeafCanonicalPath() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("explicitThisCallReturnRebinding")
        );
    }

    @Test
    void valueCallReturnRebindingShouldKeepOnlyLeafCanonicalPath() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("valueCallReturnRebinding")
        );
    }

    @Test
    void valueCallReturnRebindingShouldNotUseSideEffectsAsReturnedValue() {
        assertEquals(
                Set.of("type.code"),
                analyzePaths("valueCallReturnRebindingShouldNotUseSideEffectsAsReturnedValue")
        );
    }

    @Test
    void valueCallReturnRebindingShouldUseActualReturnedValue() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("valueCallReturnRebindingShouldUseActualReturnedValue")
        );
    }

    @Test
    void valueCallReturnRebindingShouldPreserveSideEffectsAndReturnedLeaf() {
        assertEquals(
                Set.of("type.code", "shippingAddress.city"),
                analyzePaths("valueCallReturnRebindingShouldPreserveSideEffectsAndReturnedLeaf")
        );
    }

    @Test
    void castContinuationKeepsOnlyLeafCanonicalPath() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("castContinuation")
        );
    }

    @Test
    void interprocReturnWithoutDeeperCallerAccessEmitsAssociationLeaf() {
        assertEquals(
                Set.of("shippingAddress"),
                analyzePaths("returnedAssociationWithoutDeeperCallerAccess")
        );
    }

    @Test
    void interprocReturnWithDeeperCallerAccessKeepsOnlyLeafPath() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("returnedAssociationWithDeeperCallerAccess")
        );
    }

    @Test
    void returnedParameterShouldRebindExplicitOrigin() {
        assertEquals(
                Set.of("shippingAddress.city"),
                analyzePaths("returnedParameterShouldRebindExplicitOrigin")
        );
    }

    @Test
    void helperBodyReadsShouldBePreservedForNonRebindableValueCall() {
        assertEquals(
                Set.of(
                        "liabilityLines.rate",
                        "liabilityLines.nomenclature",
                        "liabilityLines.type",
                        "liabilityLines.cost",
                        "paymentLines.rate",
                        "paymentLines.nomenclature",
                        "paymentLines.type",
                        "paymentLines.cost"
                ),
                analyzePaths(
                        "helperBodyReadsShouldBePreservedForNonRebindableValueCall",
                        "act",
                        GroupingAct.class.getSimpleName()
                )
        );
    }

    @Test
    void helperBodyReadsShouldIgnoreReturnedMapUsageForNonRebindableValueCall() {
        assertEquals(
                Set.of(
                        "liabilityLines.rate",
                        "liabilityLines.nomenclature",
                        "liabilityLines.type",
                        "liabilityLines.cost"
                ),
                analyzePaths(
                        "helperBodyReadsShouldIgnoreReturnedMapUsageForNonRebindableValueCall",
                        "act",
                        GroupingAct.class.getSimpleName()
                )
        );
    }

    @Test
    void scalarArgumentsMustNotBecomePathAnchors() {
        assertEquals(
                Set.of(
                        "dateStart",
                        "dateFinish",
                        "contract",
                        "currency"
                ),
                analyzePaths("scalarArgumentsMustNotBecomePathAnchors")
        );
    }

    @Test
    void fullChainQueryResultMustNotInheritFilterArgumentOrigins() {
        Set<String> paths = analyzePaths(
                "fullChainQueryResultMustNotInheritFilterArgumentOrigins",
                "act",
                FullChainAct.class.getSimpleName()
        );

        assertEquals(
                Set.of(
                        "dateStart",
                        "dateFinish",
                        "contract",
                        "currency"
                ),
                paths
        );

        assertFalse(paths.contains("dateStart.docLine"));
        assertFalse(paths.contains("dateStart.cost"));
        assertFalse(paths.contains("dateFinish.docLine"));
        assertFalse(paths.contains("dateFinish.cost"));
        assertFalse(paths.contains("contract.docLine"));
        assertFalse(paths.contains("contract.cost"));
        assertFalse(paths.contains("currency.docLine"));
        assertFalse(paths.contains("currency.cost"));
    }

    @Test
    void fullChainBoundaryReturnMustNotInheritFilterArgumentOrigins() {
        Set<String> paths = analyzePaths(
                "fullChainBoundaryReturnMustNotInheritFilterArgumentOrigins",
                "act",
                FullChainAct.class.getSimpleName()
        );

        assertEquals(
                Set.of(
                        "dateStart",
                        "dateFinish",
                        "contract",
                        "currency"
                ),
                paths
        );

        assertFalse(paths.contains("dateStart.docLine"));
        assertFalse(paths.contains("dateStart.cost"));
        assertFalse(paths.contains("dateFinish.docLine"));
        assertFalse(paths.contains("dateFinish.cost"));
        assertFalse(paths.contains("contract.docLine"));
        assertFalse(paths.contains("contract.cost"));
        assertFalse(paths.contains("currency.docLine"));
        assertFalse(paths.contains("currency.cost"));
    }

    private Set<String> analyzePaths(String methodName) {
        return analyzePaths(methodName, "document", Document.class.getName());
    }

    private Set<String> analyzePaths(String methodName, String rootParamName, String rootParamType) {
        AnalysisTrace analysisTrace = new AnalysisTrace();

        Path scenarioSourceRoot = Path.of("..", "fetchplan-jmix-test-scenarios", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(java.util.List.of(scenarioSourceRoot));

        SourceAnalysisCache sourceAnalysisCache = new SourceAnalysisCache(sourceRootsResolver);
        SourceMethodResolver sourceMethodResolver = new SourceMethodResolver(sourceAnalysisCache);

        SpringBeanImplementationResolver springBeanImplementationResolver = new SpringBeanImplementationResolver(
                mock(ApplicationContext.class),
                analysisTrace
        );
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

        MethodDeclaration method = sourceMethodResolver.resolve(
                ReturnRebindingFixtureService.class.getName(),
                methodName,
                rootParamName,
                rootParamType
        );

        RawTree rawTree = astPathEngine.analyze(method, rootParamName);
        return new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));
    }
}