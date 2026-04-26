package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.nio.file.Path;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.GetterPropertyAccessResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.MethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.NameExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.payload.StatementsPayloadHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.UnknownBreakPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ExpressionStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.StatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKeyFactory;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlanner;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AstPathEngineComputedGetterTest {

    @Test
    void extractsMetadataBackedPersistentGetterPath() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionOrder order) { order.getType().getCode(); }"
        );

        assertEquals(Set.of("type.code"), paths);
    }

    @Test
    void doesNotEmitComputedBusinessGetterAsCanonicalPropertyPath() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionOrder order) { order.getType().getCodeAsEnum(); }"
        );

        assertEquals(Set.of("type.code"), paths);
    }

    @Test
    void computedGetterBodyReadsAreExtractedToBackingPersistentProperty() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionType type) { type.getCodeAsEnum(); }"
        );

        assertEquals(Set.of("code"), paths);
    }

    @Test
    void instanceNameGetterExtractsAllBackingPropertyReads() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionType type) { type.getInstanceName(); }"
        );

        assertEquals(Set.of("code", "name"), paths);
    }

    @Test
    void computedGetterWithLocalVariableAssignmentExtractsBackingPropertyRead() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionType type) { type.getDisplayLabel(); }"
        );

        assertEquals(Set.of("code"), paths);
    }

    @Test
    void computedGetterWithDirectFieldAccessExtractsBackingProperties() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionType type) { type.getDirectFieldLabel(); }"
        );

        assertEquals(Set.of("code", "name"), paths);
    }

    @Test
    void computedGetterRecursionGuardPreventsInfiniteLoopAndFakePaths() {
        Set<String> paths = analyze(
                "void sample(GetterResolutionType type) { type.getRecursiveLabel(); }"
        );

        assertEquals(Set.of(), paths);
    }

    @Test
    void resolverExtractsComputedGetterBodyPropertiesDirectly() {
        Path testSourceRoot = Path.of("src", "test", "java").toAbsolutePath().normalize();
        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(testSourceRoot));
        SourceAnalysisCache sourceAnalysisCache = new SourceAnalysisCache(sourceRootsResolver);

        GetterPropertyAccessResolver resolver = new GetterPropertyAccessResolver(sourceAnalysisCache);
        CompilationUnit compilationUnit = StaticJavaParser.parse(
                "package io.github.reloadall.fetchplan.analyzer.jmix.engine.fixture;" +
                        " class SampleService { void sample(GetterResolutionType type) { type.getCodeAsEnum(); } }"
        );
        MethodDeclaration method = compilationUnit.findFirst(MethodDeclaration.class).orElseThrow();
        var call = method.findFirst(com.github.javaparser.ast.expr.MethodCallExpr.class).orElseThrow();

        assertEquals(Set.of("code"), resolver.resolvePropertyNames(call, method));
    }

    private Set<String> analyze(String methodSource) {
        InterprocCallPlanner interprocCallPlanner = mock(InterprocCallPlanner.class);
        when(interprocCallPlanner.plan(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(interprocCallPlanner.planValueCall(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        Path testSourceRoot = Path.of("src", "test", "java").toAbsolutePath().normalize();
        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(testSourceRoot));
        SourceAnalysisCache sourceAnalysisCache = new SourceAnalysisCache(sourceRootsResolver);

        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(
                new NameExpressionHandler(),
                new MethodCallExpressionHandler(sourceAnalysisCache)
        ));
        EngineContext context = new EngineContext(expressionResolver);

        StatementHandler expressionStatementHandler = new ExpressionStatementHandler(
                new UnknownBreakPolicy(),
                interprocCallPlanner
        );
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(List.of(expressionStatementHandler));

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        CompilationUnit compilationUnit = StaticJavaParser.parse(
                "package io.github.reloadall.fetchplan.analyzer.jmix.engine.fixture;" +
                        " class SampleService { " + methodSource + " }"
        );
        MethodDeclaration method = compilationUnit.findFirst(MethodDeclaration.class)
                .orElseThrow();

        String rootParamName = method.getParameter(0).getNameAsString();
        RawTree rawTree = engine.analyze(method, rootParamName);
        return new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));
    }
}