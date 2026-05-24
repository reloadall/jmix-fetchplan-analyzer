package io.github.reloadall.fetchplan.analyzer.scenario.document.fixture;

import java.util.Set;

public final class DocumentScenarioExpectedPaths {

    public static final Set<String> INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_PATHS = Set.of();

    public static final Set<String> INSPECT_DOCUMENT_WITH_UNKNOWN_BREAK_UNCERTAIN = Set.of(
            "<root>"
    );

    public static final Set<String> INSPECT_DOCUMENT = Set.of(
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

    public static final Set<String> INSPECT_DOCUMENT_BRANCH = Set.of(
            "type.code",
            "type.name"
    );

    public static final Set<String> INSPECT_FIRST_LINE = Set.of(
            "lines.quantity",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_THIS_CALL = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_VALUE_CALL = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_LOCAL_ALIAS = Set.of(
            "type.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_ALIAS_CHAIN = Set.of(
            "type.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_CAST = Set.of(
            "shippingAddress.city"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_ENTITY_CONTINUATION = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_LEAF_TO_LIST = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_LAMBDA_THEN_FILTER = Set.of(
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_ENTITY_CONTINUATION = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_LEAF_TO_LIST = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAP_BLOCK_LAMBDA_PRE_RETURN_READ = Set.of(
            "lines.product.category.code",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_COLLECTION_FOR_EACH_LAMBDA = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_LAMBDA = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FOR_EACH_BLOCK_LAMBDA = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_LAMBDA = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FILTER_METHOD_CALL_LAMBDA = Set.of(
            "lines.product.category.code",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ANY_MATCH_LAMBDA = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_ALL_MATCH_LAMBDA = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_NONE_MATCH_LAMBDA = Set.of(
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_METHOD_REFS = Set.of(
            "lines.product",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_LAMBDAS = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_MERGE = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_TO_MAP_IDENTITY_VALUE = Set.of(
            "lines.product.sku",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_LAMBDA = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_METHOD_REF = Set.of(
            "lines.product"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_DOWNSTREAM = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_COLLECT_GROUPING_BY_SUPPLIER_DOWNSTREAM = Set.of(
            "lines.product.category.code"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_TERMINAL_SCOPE_USAGE = Set.of(
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_LAMBDA = Set.of(
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_BLOCK_LAMBDA = Set.of(
            "contracts.lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FLAT_MAP_TO_LIST = Set.of(
            "contracts.lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_LAMBDA = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_METHOD_REF_TO_LIST = Set.of(
            "lines.quantity",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_COMPARING_INT_TO_LIST = Set.of(
            "lines.quantity",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_SORTED_COMPARATOR_REVERSED_TO_LIST = Set.of(
            "lines.product.sku",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAX_COMPARATOR_LAMBDA = Set.of(
            "lines.product.sku",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MIN_COMPARATOR_METHOD_REF = Set.of(
            "lines.quantity",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MAX_COMPARATOR_COMPARING_INT = Set.of(
            "lines.quantity",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_MIN_COMPARATOR_REVERSED = Set.of(
            "lines.product.sku",
            "lines"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FIND_FIRST_IF_PRESENT_AFTER_FILTER = Set.of(
            "lines.quantity",
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FIND_ANY_IF_PRESENT = Set.of(
            "lines.product.sku"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_STREAM_FIND_FIRST_IF_PRESENT_BLOCK = Set.of(
            "lines.product.sku",
            "lines.quantity"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_WORKERS = Set.of(
            "contract.number",
            "contract.customer.name"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_GETTER_ARGUMENTS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    public static final Set<String> INSPECT_DOCUMENT_WITH_FORWARDED_REPOSITORY_ARGUMENTS = Set.of(
            "dateStart",
            "dateFinish",
            "contract",
            "currency"
    );

    private DocumentScenarioExpectedPaths() {
    }
}