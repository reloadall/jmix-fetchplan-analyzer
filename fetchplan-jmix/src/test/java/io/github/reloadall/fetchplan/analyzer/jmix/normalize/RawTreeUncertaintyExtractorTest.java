package io.github.reloadall.fetchplan.analyzer.jmix.normalize;

import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawTreeUncertaintyExtractorTest {

    private final RawTreeUncertaintyExtractor extractor = new RawTreeUncertaintyExtractor();

    @Test
    void returnsRootMarkerWhenUnknownBreakOccursAtRoot() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        rawTree.addUnknownBreak(root, "tmp", UsageKind.INTERMEDIATE);

        assertEquals(Set.of("<root>"), extractor.extract(rawTree));
    }

    @Test
    void returnsCanonicalPrefixWhenUnknownBreakOccursAfterTechnicalNodes() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("order");
        RawNode lines = rawTree.addChild(root, "lines", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode element = rawTree.addChild(lines, null, FlowKind.COLLECTION_ELEMENT, null, UsageKind.INTERMEDIATE);
        RawNode alias = rawTree.addAlias(element, "line");
        rawTree.addUnknownBreak(alias, "tmp", UsageKind.INTERMEDIATE);

        assertEquals(Set.of("lines"), extractor.extract(rawTree));
    }

    @Test
    void accumulatesDistinctUncertainPrefixes() {
        RawTree rawTree = new RawTree();
        RawNode root = rawTree.createRoot("entity");
        RawNode employee = rawTree.addChild(root, "employee", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        RawNode department = rawTree.addChild(employee, "department", FlowKind.DIRECT, null, UsageKind.INTERMEDIATE);
        rawTree.addUnknownBreak(employee, "a", UsageKind.INTERMEDIATE);
        rawTree.addUnknownBreak(department, "b", UsageKind.INTERMEDIATE);

        assertEquals(Set.of("employee", "employee.department"), extractor.extract(rawTree));
    }
}