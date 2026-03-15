package io.github.reloadall.fetchplan.analyzer.jmix.debug;

import io.github.reloadall.fetchplan.analyzer.jmix.path.PathNode;
import io.github.reloadall.fetchplan.analyzer.jmix.path.PathTree;
import org.springframework.stereotype.Component;

@Component("fpa_PathTreePrinter")
public class PathTreePrinter {

    public String print(PathTree pathTree) {
        if (pathTree == null || pathTree.getRoot() == null) {
            return "<empty path tree>";
        }

        StringBuilder sb = new StringBuilder();
        for (PathNode child : pathTree.getRoot().getChildren().values()) {
            append(child, "", sb);
        }

        if (sb.isEmpty()) {
            return "<empty path tree>";
        }

        return sb.toString();
    }

    private void append(PathNode node, String prefix, StringBuilder sb) {
        String currentPath = prefix.isEmpty()
                ? node.getSegment()
                : prefix + "." + node.getSegment();

        if (node.isTerminal()) {
            sb.append(currentPath).append('\n');
        }

        for (PathNode child : node.getChildren().values()) {
            append(child, currentPath, sb);
        }
    }
}
