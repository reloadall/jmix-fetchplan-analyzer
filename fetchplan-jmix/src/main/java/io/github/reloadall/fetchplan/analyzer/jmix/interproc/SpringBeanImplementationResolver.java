package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component("fpa_SpringBeanImplementationResolver")
public class SpringBeanImplementationResolver {

    private final ApplicationContext applicationContext;
    private final AnalysisTrace analysisTrace;

    @Autowired
    public SpringBeanImplementationResolver(ApplicationContext applicationContext,
                                            AnalysisTrace analysisTrace) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext is null");
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public Optional<Class<?>> resolveImplementation(Class<?> declaredType, String injectionPointName) {
        if (declaredType == null) {
            return Optional.empty();
        }
        analysisTrace.log("SPRING: resolve bean implementation for declaredType="
                + declaredType.getName()
                + ", injectionPoint=" + injectionPointName);

        if (!declaredType.isInterface() && !Modifier.isAbstract(declaredType.getModifiers())) {
            analysisTrace.log("SPRING: declared type already concrete -> " + declaredType.getName());
            return Optional.of(declaredType);
        }

        Map<String, Object> beans = getBeansOfType(declaredType);
        analysisTrace.log("SPRING: bean candidates count = " + beans.size());
        if (beans.isEmpty()) {
            return Optional.empty();
        }

        if (beans.size() == 1) {
            Object bean = beans.values().iterator().next();
            Class<?> targetClass = resolveTargetClass(bean);
            analysisTrace.log("SPRING: selected single bean -> " + targetClass.getName());
            return Optional.of(targetClass);
        }

        if (injectionPointName != null && beans.containsKey(injectionPointName)) {
            Class<?> targetClass = resolveTargetClass(beans.get(injectionPointName));
            analysisTrace.log("SPRING: selected bean by injection point name -> " + targetClass.getName());
            return Optional.of(targetClass);
        }

        Set<Class<?>> targetClasses = beans.values().stream()
                .map(this::resolveTargetClass)
                .collect(Collectors.toSet());

        if (targetClasses.size() == 1) {
            return Optional.of(targetClasses.iterator().next());
        }

        analysisTrace.log("SPRING: ambiguous bean resolution, no unique implementation");
        return Optional.empty();
    }

    public List<Class<?>> resolveImplementations(Class<?> declaredType) {
        if (declaredType == null) {
            return List.of();
        }

        analysisTrace.log("SPRING: resolve all bean implementations for declaredType=" + declaredType.getName());

        if (!declaredType.isInterface() && !Modifier.isAbstract(declaredType.getModifiers())) {
            analysisTrace.log("SPRING: declared type already concrete for collection mode -> " + declaredType.getName());
            return List.of(declaredType);
        }

        Map<String, Object> beans = getBeansOfType(declaredType);
        if (beans.isEmpty()) {
            analysisTrace.log("SPRING: collection mode found no beans for declaredType=" + declaredType.getName());
            return List.of();
        }

        LinkedHashSet<Class<?>> implementations = beans.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .map(this::resolveTargetClass)
                .filter(this::isConcreteClass)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        analysisTrace.log("SPRING: collection mode resolved implementations = "
                + implementations.stream().map(Class::getName).toList());
        return List.copyOf(implementations);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> getBeansOfType(Class<?> type) {
        return (Map) applicationContext.getBeansOfType((Class) type);
    }

    private Class<?> resolveTargetClass(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return targetClass != null ? targetClass : bean.getClass();
    }

    private boolean isConcreteClass(Class<?> candidate) {
        return !candidate.isInterface() && !Modifier.isAbstract(candidate.getModifiers());
    }
}
