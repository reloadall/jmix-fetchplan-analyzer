package io.github.reloadall.fetchplan.analyzer.jmix.engine.visited;

import java.util.Map;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VisitedKeyFactoryTest {

    private final VisitedKeyFactory factory = new VisitedKeyFactory();

    @Test
    void differsForSameMethodAndPayloadWhenCurrentAnchorDiffers() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("document");
        RawNode contractNode = rawTree.addChild(root, "contract", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode shippingAddressNode = rawTree.addChild(root, "shippingAddress", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        MethodDeclaration method = dummyMethod();

        VisitedKey contractKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                contractNode,
                Map.of("value", ValueBinding.of(contractNode))
        ));

        VisitedKey shippingAddressKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                shippingAddressNode,
                Map.of("value", ValueBinding.of(shippingAddressNode))
        ));

        assertNotEquals(contractKey, shippingAddressKey);
    }

    @Test
    void differsForSameMethodAndPayloadWhenBindingTargetsDiffer() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("document");
        RawNode customerNode = rawTree.addChild(root, "customer", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode managerNode = rawTree.addChild(root, "manager", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        MethodDeclaration method = dummyMethod();

        VisitedKey customerKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("person", ValueBinding.of(customerNode))
        ));

        VisitedKey managerKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("person", ValueBinding.of(managerNode))
        ));

        assertNotEquals(customerKey, managerKey);
    }

    @Test
    void differsWhenDispatchTargetSetDiffers() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("document");
        MethodDeclaration method = dummyMethod();

        VisitedKey firstKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("worker", ValueBinding.forDispatchTargets(Set.of("ContractWorker", "CustomerWorker"), false))
        ));

        VisitedKey secondKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("worker", ValueBinding.forDispatchTargets(Set.of("ContractWorker"), false))
        ));

        assertNotEquals(firstKey, secondKey);
    }

    @Test
    void differsWhenTerminalOnlyFlagDiffers() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("document");
        RawNode node = rawTree.addChild(root, "type", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        MethodDeclaration method = dummyMethod();

        VisitedKey normalKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("value", ValueBinding.of(node))
        ));

        VisitedKey terminalOnlyKey = factory.build(new AnalysisStep(
                method,
                StatementsPayload.from(method),
                root,
                Map.of("value", ValueBinding.terminalOnly(Set.of(node), false))
        ));

        assertNotEquals(normalKey, terminalOnlyKey);
    }

    private MethodDeclaration dummyMethod() {
        return StaticJavaParser.parseMethodDeclaration("void sample(Object document) { helper(); }");
    }
}