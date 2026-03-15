package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import java.util.Objects;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_FetchPlanResolver")
public class FetchPlanResolver {

    private final FetchPlanRepository fetchPlanRepository;

    @Autowired
    public FetchPlanResolver(FetchPlanRepository fetchPlanRepository) {
        this.fetchPlanRepository = Objects.requireNonNull(fetchPlanRepository, "fetchPlanRepository is null");
    }

    public FetchPlan resolve(Class<?> entityClass, String fetchPlanName) {
        Objects.requireNonNull(entityClass, "entityClass is null");
        Objects.requireNonNull(fetchPlanName, "fetchPlanName is null");

        return fetchPlanRepository.getFetchPlan(entityClass, fetchPlanName);
    }
}
