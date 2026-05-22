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
import io.github.reloadall.fetchplan.analyzer.scenario.document.service.ReturnRebindingFixtureService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private Set<String> analyzePaths(String methodName) {
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
                "document",
                Document.class.getName()
        );

        RawTree rawTree = astPathEngine.analyze(method, "document");
        return new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));
    }
}