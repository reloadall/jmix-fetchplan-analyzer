package io.github.reloadall.fetchplan.analyzer.jmix.normalize;


import io.github.reloadall.fetchplan.analyzer.jmix.path.PathNode;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.FlowKind;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.UsageKind;
import org.springframework.stereotype.Component;

@Component("fpa_RawTreeNormalizer")
public class RawTreeNormalizer {

    public PathTree normalize(RawTree rawTree) {
        PathTree pathTree = new PathTree();

        if (rawTree == null || rawTree.getRoot() == null) {
            return pathTree;
        }

        visit(rawTree.getRoot(), pathTree.getRoot());
        return pathTree;
    }

    private void visit(RawNode rawNode, PathNode currentPathNode) {
        if (rawNode == null) {
            return;
        }

        PathNode nextPathNode = currentPathNode;

        FlowKind flowKind = rawNode.getFlowKind();
        if (flowKind == null) {
            return;
        }

        switch (flowKind) {
            case ROOT -> nextPathNode = currentPathNode;

            case DIRECT -> {
                String field = rawNode.getEntityField();
                if (field == null || field.isBlank()) {
                    return;
                }
                nextPathNode = currentPathNode.getOrCreateChild(field);
                nextPathNode.setLeafCandidate(true);
            }

            case COLLECTION_ELEMENT, ALIAS -> nextPathNode = currentPathNode;

            case UNKNOWN_BREAK -> {
                currentPathNode.setBlockedByUncertainty(true);
                return;
            }
        }

        if (rawNode.getUsageKind() == UsageKind.TERMINAL && nextPathNode.getSegment() != null) {
            nextPathNode.setTerminal(true);
        }

        for (RawNode child : rawNode.getChildren().values()) {
            visit(child, nextPathNode);
        }
    }
}
