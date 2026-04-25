package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.GroupInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RouteInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.VendorInfo;
import org.springframework.stereotype.Service;

@Service
public class GroupInfoFinder {

    public GroupInfo findGroupInfo(RootDocument document, RouteInfo routeInfo, VendorInfo vendorInfo, ScenarioLog log) {
        if (routeInfo != null) {
            GroupInfo groupInfo = routeInfo.getGroupInfo();
            if (groupInfo == null) {
                return null;
            }
            return groupInfo;
        }

        return null;
    }
}