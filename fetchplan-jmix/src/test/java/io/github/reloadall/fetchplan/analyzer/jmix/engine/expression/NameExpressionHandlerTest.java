package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.util.Map;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.EngineContext;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameExpressionHandlerTest {

    private final NameExpressionHandler handler = new NameExpressionHandler();
    private final EngineContext context = new EngineContext(new ExpressionResolver(java.util.List.of(handler)));

    @Test
    void resolvesBoundNameToExistingRawNode() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode typeNode = rawTree.addChild(root, "type", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        AnalysisStep step = new AnalysisStep(
                dummyMethod(),
                io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload.from(dummyMethod()),
                root,
                Map.of("alias", ValueBinding.of(typeNode))
        );

        ExpressionResolutionResult result = handler.resolveAll(
                rawTree,
                step,
                StaticJavaParser.parseExpression("alias"),
                context
        );

        assertFalse(result.isEmpty());
        assertFalse(result.isUncertain());
        assertEquals(Set.of(typeNode), result.getNodes());
    }

    @Test
    void preservesUncertaintyFlagFromBinding() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode typeNode = rawTree.addChild(root, "type", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        AnalysisStep step = new AnalysisStep(
                dummyMethod(),
                io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload.from(dummyMethod()),
                root,
                Map.of("alias", new ValueBinding(Set.of(typeNode), true))
        );

        ExpressionResolutionResult result = handler.resolveAll(
                rawTree,
                step,
                StaticJavaParser.parseExpression("alias"),
                context
        );

        assertTrue(result.isUncertain());
        assertEquals(Set.of(typeNode), result.getNodes());
    }

    @Test
    void returnsEmptyWhenNameIsNotBound() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");

        AnalysisStep step = new AnalysisStep(
                dummyMethod(),
                io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload.from(dummyMethod()),
                root,
                Map.of()
        );

        ExpressionResolutionResult result = handler.resolveAll(
                rawTree,
                step,
                StaticJavaParser.parseExpression("missing"),
                context
        );

        assertTrue(result.isEmpty());
        assertFalse(result.isUncertain());
    }

    private MethodDeclaration dummyMethod() {
        return StaticJavaParser.parseMethodDeclaration("void sample(Object entity) {} ");
    }
}