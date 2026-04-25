package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity;

public class RootDocument {

    private DocumentDetail detail;
    private RouteInfo routeInfo;

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
}