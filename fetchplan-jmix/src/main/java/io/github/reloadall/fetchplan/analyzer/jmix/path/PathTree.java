package io.github.reloadall.fetchplan.analyzer.jmix.path;

public class PathTree {

    private final PathNode root;

    public PathTree() {
        this.root = new PathNode(null);
    }

    public PathNode getRoot() {
        return root;
    }
}
