package io.github.reloadall.fetchplan.analyzer.jmix.path;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component("fpa_PathTreeFlattener")
public class PathTreeFlattener {

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

        if (node.isTerminal()) {
            result.add(currentPath);
        }

        for (PathNode child : node.getChildren().values()) {
            visit(child, currentPath, result);
        }
    }
}
