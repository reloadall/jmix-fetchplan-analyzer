package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class BaseLine {

    private String id;
    private SyntheticDocument parent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SyntheticDocument getParent() {
        return parent;
    }

    public void setParent(SyntheticDocument parent) {
        this.parent = parent;
    }
}