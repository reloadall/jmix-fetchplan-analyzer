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

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_ENTITY_CONTINUATION_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_ENTITY_CONTINUATION_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_LEAF_TO_LIST_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_LEAF_TO_LIST_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_THEN_FILTER_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_THEN_FILTER_LEAF_PATHS = Set.of(
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_ENTITY_CONTINUATION_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_ENTITY_CONTINUATION_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_LEAF_TO_LIST_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_LEAF_TO_LIST_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_PRE_RETURN_READ_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_PRE_RETURN_READ_LEAF_PATHS = Set.of(
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_COLLECTION_FOR_EACH_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_COLLECTION_FOR_EACH_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_BLOCK_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_BLOCK_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_METHOD_CALL_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_METHOD_CALL_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.category.code",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ANY_MATCH_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ANY_MATCH_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ALL_MATCH_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ALL_MATCH_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_NONE_MATCH_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_NONE_MATCH_LAMBDA_LEAF_PATHS = Set.of(
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_METHOD_REFS_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_METHOD_REFS_LEAF_PATHS = Set.of(
            "lines.product",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_LAMBDAS_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_LAMBDAS_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_MERGE_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_MERGE_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_IDENTITY_VALUE_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_IDENTITY_VALUE_LEAF_PATHS = Set.of(
            "lines",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_METHOD_REF_ALL_PATHS = Set.of(
            "lines",
            "lines.product"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_METHOD_REF_LEAF_PATHS = Set.of(
            "lines.product"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_DOWNSTREAM_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_DOWNSTREAM_LEAF_PATHS = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_SUPPLIER_DOWNSTREAM_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.category",
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_SUPPLIER_DOWNSTREAM_LEAF_PATHS = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_TERMINAL_SCOPE_USAGE_ALL_PATHS = Set.of(
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_TERMINAL_SCOPE_USAGE_LEAF_PATHS = Set.of(
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_LAMBDA_ALL_PATHS = Set.of(
            "contracts",
            "contracts.lines",
            "contracts.lines.product",
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_LAMBDA_LEAF_PATHS = Set.of(
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_BLOCK_LAMBDA_ALL_PATHS = Set.of(
            "contracts",
            "contracts.lines",
            "contracts.lines.product",
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_BLOCK_LAMBDA_LEAF_PATHS = Set.of(
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_TO_LIST_ALL_PATHS = Set.of(
            "contracts",
            "contracts.lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_TO_LIST_LEAF_PATHS = Set.of(
            "contracts.lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_LAMBDA_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_LAMBDA_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_METHOD_REF_TO_LIST_ALL_PATHS = Set.of(
            "lines",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_METHOD_REF_TO_LIST_LEAF_PATHS = Set.of(
            "lines",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_COMPARING_INT_TO_LIST_ALL_PATHS = Set.of(
            "lines",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_COMPARING_INT_TO_LIST_LEAF_PATHS = Set.of(
            "lines",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_REVERSED_TO_LIST_ALL_PATHS = Set.of(
            "lines",
            "lines.product",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_REVERSED_TO_LIST_LEAF_PATHS = Set.of(
            "lines.product.sku",
            "lines"
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