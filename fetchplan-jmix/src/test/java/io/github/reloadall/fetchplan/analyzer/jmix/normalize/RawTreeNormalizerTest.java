package io.github.reloadall.fetchplan.analyzer.jmix.normalize;

import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTreeFlattener;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawTreeNormalizerTest {

    private final RawTreeNormalizer normalizer = new RawTreeNormalizer();
    private final PathTreeFlattener flattener = new PathTreeFlattener();

    @Test
    void hidesAliasAndCollectionElementFromCanonicalOutput() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("order");
        RawNode lines = rawTree.addChild(root, "lines", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode element = rawTree.addChild(lines, null, FlowKind.COLLECTION_ELEMENT, null, UsageKind.INTERMEDIATE);
        RawNode alias = rawTree.addAlias(element, "line");
        rawTree.addChild(alias, "productCode", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of("lines.productCode"), paths);
    }

    @Test
    void stopsNormalizationAtUnknownBreak() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode type = rawTree.addChild(root, "type", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode unknown = rawTree.addUnknownBreak(type, "tmp", UsageKind.INTERMEDIATE);
        rawTree.addChild(unknown, "name", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of(), paths);
    }

    @Test
    void keepsTerminalParentPathAlongsideDeeperTerminalChild() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode type = rawTree.addChild(root, "type", FlowKind.DIRECT, null, UsageKind.TERMINAL);
        rawTree.addChild(type, "code", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of("type", "type.code"), paths);
    }

    @Test
    void emitsIntermediateAssociationAsLeafWhenNoDeeperDescendantsExist() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        rawTree.addChild(root, "shippingAddress", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of("shippingAddress"), paths);
    }

    @Test
    void suppressesIntermediateAssociationWhenOnlyDeeperLeafExists() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode shippingAddress = rawTree.addChild(root, "shippingAddress", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        rawTree.addChild(shippingAddress, "city", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of("shippingAddress.city"), paths);
    }

    @Test
    void doesNotTurnParentIntoLeafWhenDeeperAccessFallsIntoUnknownBreak() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode shippingAddress = rawTree.addChild(root, "shippingAddress", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode unknown = rawTree.addUnknownBreak(shippingAddress, "tmp", UsageKind.INTERMEDIATE);
        rawTree.addChild(unknown, "city", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of(), paths);
    }

    @Test
    void suppressesExplicitTerminalSystemLeafAndKeepsUsefulParentAnchor() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode parent = rawTree.addChild(root, "parent", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode lineBase = rawTree.addChild(parent, "lineBase", FlowKind.DIRECT, null, UsageKind.TERMINAL);
        rawTree.addChild(lineBase, "id", FlowKind.DIRECT, null, UsageKind.TERMINAL);

        Set<String> paths = flattener.flatten(normalizer.normalize(rawTree));

        assertEquals(Set.of("parent.lineBase"), paths);
    }
}