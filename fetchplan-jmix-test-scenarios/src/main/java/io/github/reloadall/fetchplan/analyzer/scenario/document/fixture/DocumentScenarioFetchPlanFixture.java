package io.github.reloadall.fetchplan.analyzer.scenario.document.fixture;

import java.util.Set;

public final class DocumentScenarioFetchPlanFixture {

    public static final Set<String> INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_ALL_PATHS = Set.of(
            "shippingAddress",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_LEAF_PATHS = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_ALL_PATHS = Set.of(
            "number",
            "type",
            "type.code",
            "type.name",
            "contract",
            "contract.number",
            "contract.customer",
            "contract.customer.name",
            "contract.customer.tier",
            "contract.customer.tier.code",
            "contract.customer.manager",
            "contract.customer.manager.email",
            "contract.customer.manager.department",
            "contract.customer.manager.department.name",
            "lines",
            "lines.quantity",
            "lines.product",
            "lines.product.sku",
            "lines.product.category",
            "lines.product.category.code",
            "shippingAddress",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_LEAF_PATHS = Set.of(
            "number",
            "type.code",
            "type.name",
            "contract.number",
            "contract.customer.name",
            "contract.customer.tier.code",
            "contract.customer.manager.email",
            "contract.customer.manager.department.name",
            "lines.quantity",
            "lines.product.sku",
            "lines.product.category.code",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_BRANCH_ALL_PATHS = Set.of(
            "type",
            "type.code",
            "type.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_BRANCH_LEAF_PATHS = Set.of(
            "type.code",
            "type.name"
    );

    public static final Set<String> INSPECT_FIRST_LINE_ALL_PATHS = Set.of(
            "lines",
            "lines.quantity",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_FIRST_LINE_LEAF_PATHS = Set.of(
            "lines.quantity",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_THIS_CALL_ALL_PATHS = Set.of(
            "shippingAddress",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_THIS_CALL_LEAF_PATHS = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_VALUE_CALL_ALL_PATHS = Set.of(
            "shippingAddress",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_VALUE_CALL_LEAF_PATHS = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOCAL_ALIAS_ALL_PATHS = Set.of(
            "type",
            "type.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOCAL_ALIAS_LEAF_PATHS = Set.of(
            "type.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_ALIAS_CHAIN_ALL_PATHS = Set.of(
            "type",
            "type.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_ALIAS_CHAIN_LEAF_PATHS = Set.of(
            "type.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CAST_ALL_PATHS = Set.of(
            "shippingAddress",
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CAST_LEAF_PATHS = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_WORKERS_ALL_PATHS = Set.of(
            "contract",
            "contract.number",
            "contract.customer",
            "contract.customer.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_WORKERS_LEAF_PATHS = Set.of(
            "contract.number",
            "contract.customer.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_GETTER_ARGUMENTS_ALL_PATHS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_GETTER_ARGUMENTS_LEAF_PATHS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_FORWARDED_REPOSITORY_ARGUMENTS_ALL_PATHS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_FORWARDED_REPOSITORY_ARGUMENTS_LEAF_PATHS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    private DocumentScenarioFetchPlanFixture() {
    }
}