package io.github.reloadall.fetchplan.analyzer.jmix.engine;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.ExpressionResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.MethodCallExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.expression.NameExpressionHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.payload.StatementsPayloadHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.policy.UnknownBreakPolicy;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ExpressionStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.ForEachStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.StatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKeyFactory;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlanner;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocMethodResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AstPathEngineForEachTest {

    @Test
    void extractsPathThroughForeachCollectionElement() {
        InterprocCallPlanner interprocCallPlanner = mock(InterprocCallPlanner.class);
        when(interprocCallPlanner.plan(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(interprocCallPlanner.planValueCall(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        NameExpressionHandler nameHandler = new NameExpressionHandler();
        MethodCallExpressionHandler methodCallHandler = new MethodCallExpressionHandler();
        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(nameHandler, methodCallHandler));
        EngineContext context = new EngineContext(expressionResolver);

        StatementHandler expressionStatementHandler = new ExpressionStatementHandler(
                new UnknownBreakPolicy(),
                interprocCallPlanner
        );
        StatementHandler forEachStatementHandler = new ForEachStatementHandler(mock(InterprocMethodResolver.class));
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, forEachStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { for (Line line : order.getLines()) { line.getProductCode(); } }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("lines.productCode"), paths);
    }

    @Test
    void keepsStandaloneCollectionGetterAsTerminalPath() {
        InterprocCallPlanner interprocCallPlanner = mock(InterprocCallPlanner.class);
        when(interprocCallPlanner.plan(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(interprocCallPlanner.planValueCall(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        NameExpressionHandler nameHandler = new NameExpressionHandler();
        MethodCallExpressionHandler methodCallHandler = new MethodCallExpressionHandler();
        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(nameHandler, methodCallHandler));
        EngineContext context = new EngineContext(expressionResolver);

        StatementHandler expressionStatementHandler = new ExpressionStatementHandler(
                new UnknownBreakPolicy(),
                interprocCallPlanner
        );
        StatementHandler forEachStatementHandler = new ForEachStatementHandler(mock(InterprocMethodResolver.class));
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, forEachStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { order.getLines(); }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("lines"), paths);
    }

    @Test
    void keepsStandaloneCollectionGetterAndLeafWhenBothUsagesExist() {
        InterprocCallPlanner interprocCallPlanner = mock(InterprocCallPlanner.class);
        when(interprocCallPlanner.plan(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(interprocCallPlanner.planValueCall(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        NameExpressionHandler nameHandler = new NameExpressionHandler();
        MethodCallExpressionHandler methodCallHandler = new MethodCallExpressionHandler();
        ExpressionResolver expressionResolver = new ExpressionResolver(List.of(nameHandler, methodCallHandler));
        EngineContext context = new EngineContext(expressionResolver);

        StatementHandler expressionStatementHandler = new ExpressionStatementHandler(
                new UnknownBreakPolicy(),
                interprocCallPlanner
        );
        StatementHandler forEachStatementHandler = new ForEachStatementHandler(mock(InterprocMethodResolver.class));
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, forEachStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { order.getLines(); for (Line line : order.getLines()) { line.getProductCode(); } }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("lines", "lines.productCode"), paths);
    }
}