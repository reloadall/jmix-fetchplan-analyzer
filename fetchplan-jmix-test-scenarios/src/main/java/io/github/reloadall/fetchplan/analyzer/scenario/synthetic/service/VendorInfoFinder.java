package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RouteInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.VendorInfo;
import org.springframework.stereotype.Service;

@Service
public class VendorInfoFinder {

    public VendorInfo findVendorInfo(RootDocument document, RouteInfo routeInfo, ScenarioLog log) {
        if (routeInfo != null) {
            VendorInfo vendorInfo = routeInfo.getVendorInfo();
            if (vendorInfo == null) {
                return null;
            }
            return vendorInfo;
        }

        return null;
    }
}