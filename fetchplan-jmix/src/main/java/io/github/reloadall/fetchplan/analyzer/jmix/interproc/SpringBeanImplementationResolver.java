package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.lang.reflect.Modifier;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> getBeansOfType(Class<?> type) {
        return (Map) applicationContext.getBeansOfType((Class) type);
    }

    private Class<?> resolveTargetClass(Object bean) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return targetClass != null ? targetClass : bean.getClass();
    }
}
