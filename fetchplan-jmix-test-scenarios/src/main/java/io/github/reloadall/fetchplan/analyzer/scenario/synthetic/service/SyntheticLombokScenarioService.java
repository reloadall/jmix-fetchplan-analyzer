package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.service;

import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RootDocument;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.RouteInfo;
import io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.ScenarioLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SyntheticLombokScenarioService {

    private final RouteInfoFinder routeInfoFinder;
    private final VendorInfoFinder vendorInfoFinder;
    private final GroupInfoFinder groupInfoFinder;
    private final AgreementFinder agreementFinder;

    public void inspectDocumentWithLombokServiceCall(RootDocument document) {
        ScenarioLog log = new ScenarioLog();
        RouteInfo routeInfo = routeInfoFinder.findRouteInfo(document, log);
        if (routeInfo != null) {
            routeInfo.getCode();
        }
    }

    public void inspectDocumentWithChainedFinders(RootDocument document) {
        ScenarioLog log = new ScenarioLog();

        RouteInfo routeInfo = routeInfoFinder.findRouteInfo(document, log);
        io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.VendorInfo vendorInfo =
                vendorInfoFinder.findVendorInfo(document, routeInfo, log);
        io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.GroupInfo groupInfo =
                groupInfoFinder.findGroupInfo(document, routeInfo, vendorInfo, log);

        if (vendorInfo != null) {
            vendorInfo.getCode();
        }

        if (groupInfo != null) {
            groupInfo.getCode();
        }
    }

    public void inspectDocumentWithMultiOriginAgreement(RootDocument document) {
        ScenarioLog log = new ScenarioLog();

        RouteInfo routeInfo = routeInfoFinder.findRouteInfo(document, log);
        io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.VendorInfo vendorInfo =
                vendorInfoFinder.findVendorInfo(document, routeInfo, log);
        io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.GroupInfo groupInfo =
                groupInfoFinder.findGroupInfo(document, routeInfo, vendorInfo, log);

        io.github.reloadall.fetchplan.analyzer.scenario.synthetic.entity.Agreement agreement =
                agreementFinder.findAgreement(vendorInfo, groupInfo, log);

        if (agreement != null) {
            agreement.getSides().getCounterparty().getName();
        }
    }
}