package io.github.reloadall.fetchplan.analyzer.jmix.debug;

import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawNode;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.springframework.stereotype.Component;

@Component("fpa_RawTreePrinter")
public class RawTreePrinter {

    public String print(RawTree rawTree) {
        if (rawTree == null) {
            return "<null raw tree>";
        }

        if (rawTree.getRoot() == null) {
            return "<empty raw tree>";
        }

        StringBuilder sb = new StringBuilder();
        appendNode(sb, rawTree.getRoot(), 0);
        return sb.toString();
    }

    private void appendNode(StringBuilder sb, RawNode node, int depth) {
        indent(sb, depth);
        sb.append('[').append(node.getId()).append("] ");
        sb.append(node.getFlowKind());

        if (node.getEntityField() != null) {
            sb.append(" field=").append(node.getEntityField());
        }

        if (node.getVariableName() != null) {
            sb.append(" var=").append(node.getVariableName());
        }

        if (node.getSourceNodeId() != null) {
            sb.append(" source=").append(node.getSourceNodeId());
        }

        sb.append(" usage=").append(node.getUsageKind());
        sb.append('\n');

        for (RawNode child : node.getChildren().values()) {
            appendNode(sb, child, depth + 1);
        }
    }

    private void indent(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
    }

}
