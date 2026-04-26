package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RouteInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import org.springframework.stereotype.Service;

@Service
public class RouteCodeReader {

    public String readCode(RouteInfo routeInfo, ScenarioLog log) {
        if (routeInfo == null) {
            return null;
        }

        return routeInfo.getCode();
    }
}