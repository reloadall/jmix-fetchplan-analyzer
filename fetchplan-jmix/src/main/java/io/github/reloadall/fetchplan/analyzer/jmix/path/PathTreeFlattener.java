package io.github.reloadall.fetchplan.analyzer.jmix.path;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component("fpa_PathTreeFlattener")
public class PathTreeFlattener {

    private static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            "id",
            "version",
            "createTs",
            "createdBy",
            "updateTs",
            "updatedBy",
            "deleteTs",
            "deletedBy"
    );

    public Set<String> flatten(PathTree pathTree) {
        Set<String> result = new LinkedHashSet<>();

        if (pathTree == null || pathTree.getRoot() == null) {
            return result;
        }

        for (PathNode child : pathTree.getRoot().getChildren().values()) {
            visit(child, "", result);
        }

        return result;
    }

    private void visit(PathNode node, String prefix, Set<String> result) {
        String currentPath = prefix.isEmpty()
                ? node.getSegment()
                : prefix + "." + node.getSegment();

        boolean hasEmittedDescendant = false;
        for (PathNode child : node.getChildren().values()) {
            int before = result.size();
            visit(child, currentPath, result);
            if (result.size() > before) {
                hasEmittedDescendant = true;
            }
        }

        if (shouldEmit(node, hasEmittedDescendant)) {
            result.add(currentPath);
        }
    }

    private boolean shouldEmit(PathNode node, boolean hasEmittedDescendant) {
        if (SYSTEM_FIELD_NAMES.contains(node.getSegment())) {
            return false;
        }

        if (node.isExplicitTerminal()) {
            return true;
        }

        if (node.isBlockedByUncertainty()) {
            return false;
        }

        return node.isLeafCandidate() && !hasEmittedDescendant;
    }
}
