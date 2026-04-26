package io.github.reloadall.fetchplan.analyzer.scenario.synthetic.fixture;

import java.util.Set;

public final class SyntheticLombokScenarioFetchPlanFixture {

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL_ALL_PATHS = Set.of(
            "detail",
            "detail.parentDetail",
            "detail.parentDetail.document",
            "detail.parentDetail.document.routeInfo",
            "detail.parentDetail.document.routeInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOMBOK_SERVICE_CALL_LEAF_PATHS = Set.of(
            "detail.parentDetail.document.routeInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CHAINED_FINDERS_ALL_PATHS = Set.of(
            "detail",
            "detail.parentDetail",
            "detail.parentDetail.document",
            "detail.parentDetail.document.routeInfo",
            "detail.parentDetail.document.routeInfo.vendorInfo",
            "detail.parentDetail.document.routeInfo.vendorInfo.code",
            "detail.parentDetail.document.routeInfo.groupInfo",
            "detail.parentDetail.document.routeInfo.groupInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CHAINED_FINDERS_LEAF_PATHS = Set.of(
            "detail.parentDetail.document.routeInfo.vendorInfo.code",
            "detail.parentDetail.document.routeInfo.groupInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT_ALL_PATHS = Set.of(
            "detail",
            "detail.parentDetail",
            "detail.parentDetail.document",
            "detail.parentDetail.document.routeInfo",
            "detail.parentDetail.document.routeInfo.vendorInfo",
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement",
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides",
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides.counterparty",
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides.counterparty.name",
            "detail.parentDetail.document.routeInfo.groupInfo",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement.sides",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement.sides.counterparty",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_MULTI_ORIGIN_AGREEMENT_LEAF_PATHS = Set.of(
            "detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides.counterparty.name",
            "detail.parentDetail.document.routeInfo.groupInfo.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST_ALL_PATHS = Set.of(
            "header",
            "header.agreement",
            "header.agreement.sides",
            "header.agreement.sides.counterparty",
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_TYPE_GUARD_AND_CAST_LEAF_PATHS = Set.of(
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST_ALL_PATHS = Set.of(
            "header",
            "header.agreement",
            "header.agreement.sides",
            "header.agreement.sides.counterparty",
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_NEGATIVE_TYPE_GUARD_AND_CAST_LEAF_PATHS = Set.of(
            "header.agreement.sides.counterparty.name"
    );

    public static final Set<String> INSPECT_LINE_WITH_BOOLEAN_HELPER_BODY_ALL_PATHS = Set.of(
            "parent",
            "parent.metaName"
    );

    public static final Set<String> INSPECT_LINE_WITH_BOOLEAN_HELPER_BODY_LEAF_PATHS = Set.of(
            "parent.metaName"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_NESTED_VALUE_CALL_ARGUMENT_ALL_PATHS = Set.of(
            "detail",
            "detail.parentDetail",
            "detail.parentDetail.document",
            "detail.parentDetail.document.routeInfo",
            "detail.parentDetail.document.routeInfo.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_NESTED_VALUE_CALL_ARGUMENT_LEAF_PATHS = Set.of(
            "detail.parentDetail.document.routeInfo.code"
    );

    private SyntheticLombokScenarioFetchPlanFixture() {
    }
}