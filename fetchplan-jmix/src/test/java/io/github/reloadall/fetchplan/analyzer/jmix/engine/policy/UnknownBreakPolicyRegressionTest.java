package io.github.reloadall.fetchplan.analyzer.jmix.engine.policy;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
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
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeUncertaintyExtractor;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service.SyntheticLombokScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnknownBreakPolicyRegressionTest {

    @Test
    void pureHelperObjectCreationInitializerShouldNotCreateUnknownBreak() {
        AnalysisOutcome outcome = analyzeParsedMethod(
                "void sample(RootDocument document) { ScenarioLog log = new ScenarioLog(); }",
                "document"
        );

        assertEquals(Set.of(), outcome.analyzedPaths());
        assertEquals(Set.of(), outcome.uncertainPaths());
    }

    @Test
    void interprocNullReturnsShouldNotCreateUnknownBreak() {
        AnalysisOutcome outcome = analyzeScenarioMethod(
                SyntheticLombokScenarioService.class,
                "inspectDocumentWithLombokServiceCall",
                "document",
                RootDocument.class
        );

        assertEquals(Set.of("detail.parentDetail.document.routeInfo.code"), outcome.analyzedPaths());
        assertEquals(Set.of(), outcome.uncertainPaths());
    }

    @Test
    void unsupportedStaticHelperExpressionShouldStillProduceUncertainty() {
        AnalysisOutcome outcome = analyzeParsedMethod(
                "void sample(Document document) { Address address = AddressSelector.select(document); address.getCity(); }",
                "document"
        );

        assertEquals(Set.of(), outcome.analyzedPaths());
        assertEquals(Set.of("<root>"), outcome.uncertainPaths());
    }

    @Test
    void unresolvedDynamicMethodCallResultShouldStillProduceUncertainty() {
        AnalysisOutcome outcome = analyzeParsedMethod(
                "void sample(Document document) { SomeEntity x = unknownProvider(document); x.getName(); }",
                "document"
        );

        assertEquals(Set.of(), outcome.analyzedPaths());
        assertEquals(Set.of("<root>"), outcome.uncertainPaths());
    }

    @Test
    void harmlessBooleanConditionWithoutEntityPathShouldNotCreateUnknownBreak() {
        AnalysisOutcome outcome = analyzeParsedMethod(
                "void sample(Document document) { if (true) { return; } }",
                "document"
        );

        assertEquals(Set.of(), outcome.analyzedPaths());
        assertEquals(Set.of(), outcome.uncertainPaths());
    }

    private AnalysisOutcome analyzeParsedMethod(String methodSource, String rootParamName) {
        InterprocCallPlanner interprocCallPlanner = mock(InterprocCallPlanner.class);
        when(interprocCallPlanner.plan(any(), any(), any(), any())).thenReturn(java.util.Optional.empty());
        when(interprocCallPlanner.planValueCall(any(), any(), any(), any(), any())).thenReturn(java.util.Optional.empty());
        when(interprocCallPlanner.planFanOut(any(), any(), any(), any())).thenReturn(java.util.Optional.empty());

        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(
                new NameExpressionHandler(),
                new MethodCallExpressionHandler()
        ));

        AstPathEngine engine = new AstPathEngine(
                List.of(new StatementsPayloadHandler(List.of(
                        new ExpressionStatementHandler(new UnknownBreakPolicy(), interprocCallPlanner),
                        new IfStatementHandler()
                ))),
                new EngineContext(expressionResolver),
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(methodSource);
        RawTree rawTree = engine.analyze(method, rootParamName);
        return new AnalysisOutcome(
                new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree)),
                new RawTreeUncertaintyExtractor().extract(rawTree)
        );
    }

    private AnalysisOutcome analyzeScenarioMethod(Class<?> targetClass,
                                                  String methodName,
                                                  String rootParamName,
                                                  Class<?> rootParamType) {
        AnalysisTrace analysisTrace = new AnalysisTrace();

        Path scenarioSourceRoot = Path.of("..", "fetchplan-jmix-test-scenarios", "src", "main", "java")
                .toAbsolutePath()
                .normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(scenarioSourceRoot));

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

        MethodDeclaration method = sourceMethodResolver.resolve(
                targetClass.getName(),
                methodName,
                rootParamName,
                rootParamType.getName()
        );

        RawTree rawTree = astPathEngine.analyze(method, rootParamName);
        return new AnalysisOutcome(
                new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree)),
                new RawTreeUncertaintyExtractor().extract(rawTree)
        );
    }

    private record AnalysisOutcome(Set<String> analyzedPaths, Set<String> uncertainPaths) {
    }
}