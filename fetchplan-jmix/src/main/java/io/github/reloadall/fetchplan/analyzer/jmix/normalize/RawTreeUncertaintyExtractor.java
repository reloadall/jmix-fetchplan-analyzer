package io.github.reloadall.fetchplan.analyzer.jmix.normalize;

import java.util.LinkedHashSet;
import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.stereotype.Component;

@Component("fpa_RawTreeUncertaintyExtractor")
public class RawTreeUncertaintyExtractor {

    public Set<String> extract(RawTree rawTree) {
        Set<String> result = new LinkedHashSet<>();

        if (rawTree == null || rawTree.getRoot() == null) {
            return result;
        }

        visit(rawTree.getRoot(), "", result);
        return result;
    }

    private void visit(RawNode rawNode, String currentPath, Set<String> result) {
        if (rawNode == null || rawNode.getFlowKind() == null) {
            return;
        }

        String nextPath = currentPath;

        switch (rawNode.getFlowKind()) {
            case ROOT -> nextPath = currentPath;

            case DIRECT -> {
                String field = rawNode.getEntityField();
                if (field == null || field.isBlank()) {
                    return;
                }
                nextPath = currentPath.isEmpty() ? field : currentPath + "." + field;
            }

            case COLLECTION_ELEMENT, ALIAS -> nextPath = currentPath;

            case UNKNOWN_BREAK -> {
                result.add(currentPath.isBlank() ? "<root>" : currentPath);
                return;
            }
        }

        for (RawNode child : rawNode.getChildren().values()) {
            visit(child, nextPath, result);
        }
    }
}
