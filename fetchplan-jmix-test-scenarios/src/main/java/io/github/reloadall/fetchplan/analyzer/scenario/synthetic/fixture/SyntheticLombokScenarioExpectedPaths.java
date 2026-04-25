package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.fixture;

import java.util.Set;

public final class SyntheticLombokScenarioExpectedPaths {

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL = Set.of(
            "detail.parentDetail.document.routeInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CHAINED_FINDERS = Set.of(
            "detail.parentDetail.document.routeInfo.vendorInfo.code",
            "detail.parentDetail.document.routeInfo.groupInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT = Set.of(
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides.counterparty.name",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement.sides.counterparty.name"
    );

    private SyntheticLombokScenarioExpectedPaths() {
    }
}