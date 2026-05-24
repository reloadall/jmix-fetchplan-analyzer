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
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.IfStatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.statement.StatementHandler;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.visited.VisitedKeyFactory;
import io.github.reloadall.fetchplan.analyzer.jmix.interproc.InterprocCallPlanner;
import io.github.reloadall.fetchplan.analyzer.jmix.normalize.RawTreeNormalizer;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AstPathEngineIfElseTest {

    @Test
    void extractsPathsFromBothIfElseBranches() {
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
        StatementHandler ifStatementHandler = new IfStatementHandler();
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, ifStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { " +
                        "if (flag) { order.getType().getCode(); } else { order.getCustomer().getName(); }" +
                        " }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("type.code", "customer.name"), paths);
    }

    @Test
    void propagatesAlternativeBranchBindingsAfterIfElse() {
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
        StatementHandler ifStatementHandler = new IfStatementHandler();
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, ifStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Document document) { " +
                        "Address address; " +
                        "if (flag) { address = document.getShippingAddress(); } " +
                        "else { address = document.getBillingAddress(); } " +
                        "address.getCity();" +
                        " }"
        );

        RawTree rawTree = engine.analyze(method, "document");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("shippingAddress.city", "billingAddress.city"), paths);
    }

    @Test
    void propagatesInitialAndThenBranchBindingsAfterIfWithoutElse() {
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
        StatementHandler ifStatementHandler = new IfStatementHandler();
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, ifStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Document document) { " +
                        "Address address = document.getShippingAddress(); " +
                        "if (flag) { address = document.getBillingAddress(); } " +
                        "address.getCity();" +
                        " }"
        );

        RawTree rawTree = engine.analyze(method, "document");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("shippingAddress.city", "billingAddress.city"), paths);
    }

    @Test
    void extractsConditionEntityPathWithoutChangingBranchExtraction() {
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
        StatementHandler ifStatementHandler = new IfStatementHandler();
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, ifStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { " +
                        "if (order.getType().isArchived()) { order.getCustomer().getName(); } " +
                        "else { order.getType().getCode(); }" +
                        " }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("type.archived", "customer.name", "type.code"), paths);
    }

    @Test
    void nullCheckAssociationAccessEmitsAssociationLeafPath() {
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
        StatementHandler ifStatementHandler = new IfStatementHandler();
        StatementsPayloadHandler payloadHandler = new StatementsPayloadHandler(
                List.of(expressionStatementHandler, ifStatementHandler)
        );

        AstPathEngine engine = new AstPathEngine(
                List.of(payloadHandler),
                context,
                new VisitedKeyFactory(),
                new AnalysisTrace()
        );

        MethodDeclaration method = StaticJavaParser.parseMethodDeclaration(
                "void sample(Order order) { if (order.getType() != null) { } }"
        );

        RawTree rawTree = engine.analyze(method, "order");
        Set<String> paths = new PathTreeFlattener().flatten(new RawTreeNormalizer().normalize(rawTree));

        assertEquals(Set.of("type"), paths);
    }
}