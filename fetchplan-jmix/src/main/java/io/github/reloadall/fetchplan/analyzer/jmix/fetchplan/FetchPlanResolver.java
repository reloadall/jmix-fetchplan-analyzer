package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import java.util.Objects;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_FetchPlanResolver")
public class FetchPlanResolver {

    // Inject lazily: a running Jmix context provides FetchPlanRepository, but the standalone
    // single-method analysis CLI bootstraps a reduced context without it. Resolving the bean
    // eagerly would fail context refresh there; ObjectProvider keeps this bean constructible.
    private final ObjectProvider<FetchPlanRepository> fetchPlanRepositoryProvider;

    @Autowired
    public FetchPlanResolver(ObjectProvider<FetchPlanRepository> fetchPlanRepositoryProvider) {
        this.fetchPlanRepositoryProvider =
                Objects.requireNonNull(fetchPlanRepositoryProvider, "fetchPlanRepositoryProvider is null");
    }

    public FetchPlan resolve(Class<?> entityClass, String fetchPlanName) {
        Objects.requireNonNull(entityClass, "entityClass is null");
        Objects.requireNonNull(fetchPlanName, "fetchPlanName is null");

        FetchPlanRepository fetchPlanRepository = fetchPlanRepositoryProvider.getIfAvailable();
        if (fetchPlanRepository == null) {
            throw new IllegalStateException(
                    "FetchPlanRepository bean is not available: named fetch-plan resolution requires a running "
                            + "Jmix application context and is unavailable in the standalone single-method "
                            + "analysis CLI.");
        }

        return fetchPlanRepository.getFetchPlan(entityClass, fetchPlanName);
    }
}
