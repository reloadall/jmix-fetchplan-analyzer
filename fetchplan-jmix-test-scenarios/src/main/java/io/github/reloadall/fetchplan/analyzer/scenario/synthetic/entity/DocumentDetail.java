package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class DocumentDetail {

    private DocumentDetail parentDetail;
    private RootDocument document;

    public DocumentDetail getParentDetail() {
        return parentDetail;
    }

    public void setParentDetail(DocumentDetail parentDetail) {
        this.parentDetail = parentDetail;
    }

    public RootDocument getDocument() {
        return document;
    }

    public void setDocument(RootDocument document) {
        this.document = document;
    }
}