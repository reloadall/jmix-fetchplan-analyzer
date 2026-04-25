package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.DocumentDetail;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RouteInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import org.springframework.stereotype.Service;

@Service
public class RouteInfoFinder {

    public RouteInfo findRouteInfo(RootDocument document, ScenarioLog log) {
        DocumentDetail detail = document.getDetail();
        if (detail == null) {
            return null;
        }

        DocumentDetail parentDetail = detail.getParentDetail();
        if (parentDetail == null) {
            return null;
        }

        RootDocument nestedDocument = parentDetail.getDocument();
        if (nestedDocument == null) {
            return null;
        }

        RouteInfo routeInfo = nestedDocument.getRouteInfo();
        if (routeInfo == null) {
            return null;
        }

        return routeInfo;
    }
}