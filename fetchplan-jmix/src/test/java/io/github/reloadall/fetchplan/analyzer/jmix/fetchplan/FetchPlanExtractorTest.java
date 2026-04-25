package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import java.util.List;
import java.util.Set;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FetchPlanExtractorTest {

    private final FetchPlanExtractor extractor = new FetchPlanExtractor();

    @Test
    void returnsEmptySetsForNullFetchPlan() {
        FetchPlanPathSet result = extractor.extract(null);

        assertEquals(Set.of(), result.getAllPaths());
        assertEquals(Set.of(), result.getLeafPaths());
    }

    @Test
    void extractsAllAndLeafPathsForNestedFetchPlan() {
        FetchPlan nested = fetchPlan(
                property("code", null),
                property("name", null)
        );
        FetchPlan root = fetchPlan(
                property("type", nested),
                property("status", null)
        );

        FetchPlanPathSet result = extractor.extract(root);

        assertEquals(Set.of("type", "type.code", "type.name", "status"), result.getAllPaths());
        assertEquals(Set.of("type.code", "type.name", "status"), result.getLeafPaths());
    }

    @Test
    void treatsPropertyWithEmptyNestedFetchPlanAsLeaf() {
        FetchPlan emptyNested = fetchPlan();
        FetchPlan root = fetchPlan(property("customer", emptyNested));

        FetchPlanPathSet result = extractor.extract(root);

        assertEquals(Set.of("customer"), result.getAllPaths());
        assertEquals(Set.of("customer"), result.getLeafPaths());
    }

    @Test
    void extractsMultipleSiblingBranchesIndependently() {
        FetchPlan address = fetchPlan(
                property("city", null),
                property("zip", null)
        );
        FetchPlan manager = fetchPlan(
                property("email", null)
        );
        FetchPlan root = fetchPlan(
                property("address", address),
                property("manager", manager)
        );

        FetchPlanPathSet result = extractor.extract(root);

        assertEquals(
                Set.of("address", "address.city", "address.zip", "manager", "manager.email"),
                result.getAllPaths()
        );
        assertEquals(Set.of("address.city", "address.zip", "manager.email"), result.getLeafPaths());
    }

    private FetchPlan fetchPlan(FetchPlanProperty... properties) {
        FetchPlan fetchPlan = mock(FetchPlan.class);
        when(fetchPlan.getProperties()).thenReturn(List.of(properties));
        return fetchPlan;
    }

    private FetchPlanProperty property(String name, FetchPlan nested) {
        FetchPlanProperty property = mock(FetchPlanProperty.class);
        when(property.getName()).thenReturn(name);
        when(property.getFetchPlan()).thenReturn(nested);
        return property;
    }
}