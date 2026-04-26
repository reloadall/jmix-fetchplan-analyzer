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

    public static final Set<String> INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST = Set.of(
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST = Set.of(
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_BOOLEAN_HELPER_BODY = Set.of(
            "parent.metaName"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_NESTED_VALUE_CALL_ARGUMENT = Set.of(
            "detail.parentDetail.document.routeInfo.code"
    );

    private SyntheticLombokScenarioExpectedPaths() {
    }
}