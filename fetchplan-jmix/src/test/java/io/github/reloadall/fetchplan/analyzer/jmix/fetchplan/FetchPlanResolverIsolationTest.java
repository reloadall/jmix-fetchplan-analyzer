package io.github.reloadall.fetchplan.analyzer.jmix.fetchplan;

import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression guard for the standalone analysis scenario (e.g. the single-method CLI), where the
 * analyzer beans are scanned without a running Jmix application context. {@link FetchPlanResolver}
 * must stay constructible when no {@link FetchPlanRepository} bean is present, so that context
 * refresh does not fail before any analysis runs; named fetch-plan resolution may fail only when
 * actually invoked.
 */
class FetchPlanResolverIsolationTest {

    @Configuration
    @ComponentScan(basePackageClasses = FetchPlanResolver.class)
    static class IsolatedFetchPlanScanConfiguration {
    }

    @Test
    void contextWithoutFetchPlanRepositoryRefreshesAndResolverLoads() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(IsolatedFetchPlanScanConfiguration.class)) {
            FetchPlanResolver resolver = context.getBean(FetchPlanResolver.class);
            assertNotNull(resolver);
        }
    }

    @Test
    void resolveWithoutFetchPlanRepositoryThrowsClearError() {
        @SuppressWarnings("unchecked")
        ObjectProvider<FetchPlanRepository> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);

        FetchPlanResolver resolver = new FetchPlanResolver(emptyProvider);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> resolver.resolve(Object.class, "_base"));
        assertTrue(exception.getMessage().contains("FetchPlanRepository"),
                "Expected a message naming the missing FetchPlanRepository, got: " + exception.getMessage());
    }

    @Test
    void resolveDelegatesToRepositoryWhenAvailable() {
        FetchPlan fetchPlan = mock(FetchPlan.class);
        FetchPlanRepository repository = mock(FetchPlanRepository.class);
        when(repository.getFetchPlan(Object.class, "_base")).thenReturn(fetchPlan);

        @SuppressWarnings("unchecked")
        ObjectProvider<FetchPlanRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repository);

        FetchPlanResolver resolver = new FetchPlanResolver(provider);

        assertSame(fetchPlan, resolver.resolve(Object.class, "_base"));
    }
}
