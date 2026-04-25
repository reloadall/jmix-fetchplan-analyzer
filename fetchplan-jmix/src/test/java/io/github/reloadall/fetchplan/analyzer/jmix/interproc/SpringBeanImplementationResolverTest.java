package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringBeanImplementationResolverTest {

    @Test
    void resolvesAllImplementationsInDeterministicBeanNameOrder() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Map<String, Object> beans = new LinkedHashMap<>();
        beans.put("customerWorker", new CustomerWorkerBean());
        beans.put("contractWorker", new ContractWorkerBean());
        when(applicationContext.getBeansOfType(Worker.class)).thenReturn((Map) beans);

        SpringBeanImplementationResolver resolver = new SpringBeanImplementationResolver(
                applicationContext,
                new io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace()
        );

        List<Class<?>> implementations = resolver.resolveImplementations(Worker.class);

        assertEquals(List.of(ContractWorkerBean.class, CustomerWorkerBean.class), implementations);
    }

    @Test
    void resolveImplementationRemainsConservativeForMultipleDifferentBeans() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Map<String, Object> beans = new LinkedHashMap<>();
        beans.put("contractWorker", new ContractWorkerBean());
        beans.put("customerWorker", new CustomerWorkerBean());
        when(applicationContext.getBeansOfType(Worker.class)).thenReturn((Map) beans);

        SpringBeanImplementationResolver resolver = new SpringBeanImplementationResolver(
                applicationContext,
                new io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace()
        );

        Optional<Class<?>> implementation = resolver.resolveImplementation(Worker.class, null);

        assertTrue(implementation.isEmpty());
    }

    @Test
    void resolveImplementationStillSelectsBeanByInjectionPointName() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Map<String, Object> beans = new LinkedHashMap<>();
        beans.put("contractWorker", new ContractWorkerBean());
        beans.put("customerWorker", new CustomerWorkerBean());
        when(applicationContext.getBeansOfType(Worker.class)).thenReturn((Map) beans);

        SpringBeanImplementationResolver resolver = new SpringBeanImplementationResolver(
                applicationContext,
                new io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace()
        );

        Optional<Class<?>> implementation = resolver.resolveImplementation(Worker.class, "customerWorker");

        assertEquals(Optional.of(CustomerWorkerBean.class), implementation);
    }

    interface Worker {
    }

    static class ContractWorkerBean implements Worker {
    }

    static class CustomerWorkerBean implements Worker {
    }
}