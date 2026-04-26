package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class SyntheticDocument implements HasSyntheticMeta {

    private String metaName;

    @Override
    public String getMetaName() {
        return metaName;
    }

    public void setMetaName(String metaName) {
        this.metaName = metaName;
    }
}