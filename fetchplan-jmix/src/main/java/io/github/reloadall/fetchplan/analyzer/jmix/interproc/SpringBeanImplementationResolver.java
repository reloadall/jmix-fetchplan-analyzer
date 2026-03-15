package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("fpa_SpringBeanImplementationResolver")
public class SpringBeanImplementationResolver {

    private final ApplicationContext applicationContext;

    @Autowired
    public SpringBeanImplementationResolver(ApplicationContext applicationContext) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext is null");
    }

    public Optional<Class<?>> resolveImplementation(Class<?> declaredType, String injectionPointName) {
        if (declaredType == null) {
            return Optional.empty();
        }

        if (!declaredType.isInterface() && !Modifier.isAbstract(declaredType.getModifiers())) {
            return Optional.of(declaredType);
        }

        Map<String, Object> beans = getBeansOfType(declaredType);
        if (beans.isEmpty()) {
            return Optional.empty();
        }

        if (beans.size() == 1) {
            Object bean = beans.values().iterator().next();
            return Optional.of(resolveTargetClass(bean));
        }

        if (injectionPointName != null && beans.containsKey(injectionPointName)) {
            return Optional.of(resolveTargetClass(beans.get(injectionPointName)));
        }

        Set<Class<?>> targetClasses = beans.values().stream()
                .map(this::resolveTargetClass)
                .collect(Collectors.toSet());

        if (targetClasses.size() == 1) {
            return Optional.of(targetClasses.iterator().next());
        }

        return Optional.empty();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> getBeansOfType(Class<?> type) {
        return (Map) applicationContext.getBeansOfType((Class) type);
    }

    private Class<?> resolveTargetClass(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return targetClass != null ? targetClass : bean.getClass();
    }
}
