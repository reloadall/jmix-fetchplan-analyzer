package io.github.reloadall.fetchplan.analyzer.jmix.compare;

import java.util.Set;

import io.github.reloadall.fetchplan.analyzer.jmix.fetchplan.FetchPlanPathSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathComparatorTest {

    private final PathComparator comparator = new PathComparator();

    @Test
    void marksExactDeclaredPathAsCovered() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("type", "type.code"),
                Set.of("type.code")
        );

        PathComparisonResult result = comparator.compare(
                Set.of("type.code"),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of("type.code"), result.getMatchedPaths());
        assertEquals(Set.of(), result.getMissingPaths());
        assertEquals(Set.of(), result.getExtraPaths());
        assertEquals(Set.of(), result.getUncertainPaths());
    }

    @Test
    void marksParentAnalyzedPathAsCoveredWhenDeeperDeclaredLeafExists() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("employee", "employee.department", "employee.department.name"),
                Set.of("employee.department.name")
        );

        PathComparisonResult result = comparator.compare(
                Set.of("employee.department"),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of("employee.department"), result.getMatchedPaths());
        assertEquals(Set.of(), result.getMissingPaths());
        assertEquals(Set.of("employee.department.name"), result.getExtraPaths());
        assertEquals(Set.of(), result.getUncertainPaths());
    }

    @Test
    void removesStructuralParentFromMissingWhenDeeperMissingExists() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(Set.of(), Set.of());

        PathComparisonResult result = comparator.compare(
                Set.of("employee", "employee.department", "employee.department.name"),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of("employee.department.name"), result.getMissingPaths());
    }

    @Test
    void movesDeclaredLeafUnderUncertainPrefixFromExtraToUncertain() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("subcontracts", "subcontracts.number"),
                Set.of("subcontracts.number")
        );

        PathComparisonResult result = comparator.compare(
                Set.of(),
                fetchPlanPathSet,
                Set.of("subcontracts")
        );

        assertEquals(Set.of(), result.getExtraPaths());
        assertEquals(Set.of("subcontracts", "subcontracts.number"), result.getUncertainPaths());
    }

    @Test
    void treatsRootUncertaintyAsCoveringAllDeclaredLeavesForExtraCalculation() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("type", "type.code", "employee", "employee.email"),
                Set.of("type.code", "employee.email")
        );

        PathComparisonResult result = comparator.compare(
                Set.of(),
                fetchPlanPathSet,
                Set.of("<root>")
        );

        assertEquals(Set.of(), result.getExtraPaths());
        assertEquals(Set.of("<root>", "type.code", "employee.email"), result.getUncertainPaths());
    }
}