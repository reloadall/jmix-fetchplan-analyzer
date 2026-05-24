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