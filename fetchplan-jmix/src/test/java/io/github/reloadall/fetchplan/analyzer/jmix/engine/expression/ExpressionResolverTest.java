package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionResolverTest {

    @Test
    void firstMeaningfulHandlerWinsAndLaterHandlersDoNotAlterResult() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode firstNode = rawTree.addChild(root, "first", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode secondNode = rawTree.addChild(root, "second", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        ExpressionHandler firstHandler = new StubHandler(ExpressionResolutionResult.of(firstNode));
        ExpressionHandler secondHandler = new StubHandler(ExpressionResolutionResult.of(secondNode));

        ExpressionResolver resolver = new ExpressionResolver(List.of(firstHandler, secondHandler));
        EngineContext context = new EngineContext(resolver);
        AnalysisStep step = new AnalysisStep(dummyMethod(), StatementsPayload.from(dummyMethod()), root, Map.of());

        ExpressionResolutionResult result = resolver.resolveAll(
                rawTree,
                step,
                StaticJavaParser.parseExpression("value"),
                context
        );

        assertEquals(Set.of(firstNode), result.getNodes());
        assertFalse(result.isUncertain());
    }

    @Test
    void uncertainFirstMeaningfulHandlerAlsoStopsResolution() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode laterNode = rawTree.addChild(root, "later", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        ExpressionHandler firstHandler = new StubHandler(ExpressionResolutionResult.uncertainEmpty());
        ExpressionHandler secondHandler = new StubHandler(ExpressionResolutionResult.of(laterNode));

        ExpressionResolver resolver = new ExpressionResolver(List.of(firstHandler, secondHandler));
        EngineContext context = new EngineContext(resolver);
        AnalysisStep step = new AnalysisStep(dummyMethod(), StatementsPayload.from(dummyMethod()), root, Map.of());

        ExpressionResolutionResult result = resolver.resolveAll(
                rawTree,
                step,
                StaticJavaParser.parseExpression("value"),
                context
        );

        assertTrue(result.isUncertain());
        assertTrue(result.isEmpty());
    }

    private MethodDeclaration dummyMethod() {
        return StaticJavaParser.parseMethodDeclaration("void sample(Object entity) {} ");
    }

    private static final class StubHandler implements ExpressionHandler {
        private final ExpressionResolutionResult result;

        private StubHandler(ExpressionResolutionResult result) {
            this.result = result;
        }

        @Override
        public boolean supports(Expression expression) {
            return true;
        }

        @Override
        public ExpressionResolutionResult resolveAll(RawTree rawTree,
                                                     AnalysisStep step,
                                                     Expression expression,
                                                     EngineContext context) {
            return result;
        }
    }
}