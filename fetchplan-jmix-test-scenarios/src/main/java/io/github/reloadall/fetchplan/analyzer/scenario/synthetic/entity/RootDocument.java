package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class RootDocument implements HasSyntheticDocument {

    private DocumentDetail detail;
    private RouteInfo routeInfo;
    private Agreement agreement;

    public DocumentDetail getDetail() {
        return detail;
    }

    public void setDetail(DocumentDetail detail) {
        this.detail = detail;
    }

    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    public void setRouteInfo(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
    }

    @Override
    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }
}