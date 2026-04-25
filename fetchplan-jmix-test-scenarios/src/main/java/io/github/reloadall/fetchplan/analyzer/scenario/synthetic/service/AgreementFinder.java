package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.Agreement;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.GroupInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.VendorInfo;
import org.springframework.stereotype.Service;

@Service
public class AgreementFinder {

    public Agreement findAgreement(VendorInfo vendorInfo, GroupInfo groupInfo, ScenarioLog log) {
        if (vendorInfo != null) {
            Agreement agreement = vendorInfo.getAgreement();
            if (agreement == null) {
                return null;
            }
            return agreement;
        }

        if (groupInfo == null) {
            return null;
        }

        Agreement agreement = groupInfo.getAgreement();
        if (agreement == null) {
            return null;
        }

        return agreement;
    }
}