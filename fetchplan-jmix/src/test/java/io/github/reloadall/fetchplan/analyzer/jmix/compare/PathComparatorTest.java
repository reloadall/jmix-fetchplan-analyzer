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
    void doesNotInflateCoveredWithDeclaredStructuralParentWhenOnlyDeeperLeafIsAnalyzed() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("agreement", "agreement.sides", "agreement.sides.counterparty", "agreement.sides.counterparty.name"),
                Set.of("agreement.sides.counterparty.name")
        );

        PathComparisonResult result = comparator.compare(
                Set.of("agreement.sides.counterparty.name"),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of("agreement.sides.counterparty.name"), result.getMatchedPaths());
        assertEquals(Set.of(), result.getMissingPaths());
        assertEquals(Set.of(), result.getExtraPaths());
        assertEquals(Set.of(), result.getUncertainPaths());
    }

    @Test
    void suppressesStructuralParentFromMissingWhenDeeperDeclaredLeafIsAnalyzed() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("agreement", "agreement.sides", "agreement.sides.counterparty", "agreement.sides.counterparty.name"),
                Set.of("agreement.sides.counterparty.name")
        );

        PathComparisonResult result = comparator.compare(
                Set.of("agreement.sides.counterparty.name"),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of(), result.getMissingPaths());
    }

    @Test
    void keepsRealDeclaredLeafMissingWhenNoAnalyzedLeafExists() {
        FetchPlanPathSet fetchPlanPathSet = new FetchPlanPathSet(
                Set.of("agreement", "agreement.sides", "agreement.sides.counterparty", "agreement.sides.counterparty.name"),
                Set.of("agreement.sides.counterparty.name")
        );

        PathComparisonResult result = comparator.compare(
                Set.of(),
                fetchPlanPathSet,
                Set.of()
        );

        assertEquals(Set.of(), result.getMatchedPaths());
        assertEquals(Set.of("agreement.sides.counterparty.name"), result.getExtraPaths());
        assertEquals(Set.of(), result.getMissingPaths());
        assertEquals(Set.of(), result.getUncertainPaths());
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