package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import java.util.LinkedHashSet;
import java.util.Set;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanProperty;
import org.springframework.stereotype.Component;

@Component("fpa_FetchPlanExtractor")
public class FetchPlanExtractor {

    public FetchPlanPathSet extract(FetchPlan fetchPlan) {
        Set<String> allPaths = new LinkedHashSet<>();
        Set<String> leafPaths = new LinkedHashSet<>();

        if (fetchPlan == null) {
            return new FetchPlanPathSet(allPaths, leafPaths);
        }

        visit(fetchPlan, "", allPaths, leafPaths);
        return new FetchPlanPathSet(allPaths, leafPaths);
    }

    private void visit(FetchPlan fetchPlan,
                       String prefix,
                       Set<String> allPaths,
                       Set<String> leafPaths) {
        for (FetchPlanProperty property : fetchPlan.getProperties()) {
            String currentPath = prefix.isEmpty()
                    ? property.getName()
                    : prefix + "." + property.getName();

            allPaths.add(currentPath);

            FetchPlan nestedFetchPlan = property.getFetchPlan();
            if (nestedFetchPlan != null && !nestedFetchPlan.getProperties().isEmpty()) {
                visit(nestedFetchPlan, currentPath, allPaths, leafPaths);
            } else {
                leafPaths.add(currentPath);
            }
        }
    }
}
