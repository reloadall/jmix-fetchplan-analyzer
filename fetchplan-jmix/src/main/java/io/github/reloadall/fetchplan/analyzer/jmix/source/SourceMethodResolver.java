package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.util.List;
import java.util.Objects;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_SourceMethodResolver")
public class SourceMethodResolver {

    private final SourceAnalysisCache sourceAnalysisCache;

    @Autowired
    public SourceMethodResolver(SourceAnalysisCache sourceAnalysisCache) {
        this.sourceAnalysisCache = Objects.requireNonNull(sourceAnalysisCache, "sourceAnalysisCache is null");
    }

    public MethodDeclaration resolve(String targetClassName,
                                     String methodName,
                                     String rootParamName,
                                     String rootParamType) {
        Objects.requireNonNull(targetClassName, "targetClassName is null");
        Objects.requireNonNull(methodName, "methodName is null");
        Objects.requireNonNull(rootParamName, "rootParamName is null");
        Objects.requireNonNull(rootParamType, "rootParamType is null");

        CompilationUnit compilationUnit = sourceAnalysisCache.findJavaFile(targetClassName)
                .map(sourceAnalysisCache::getCompilationUnit)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Java source file not found for class: " + targetClassName
                ));

        String targetSimpleName = simpleName(targetClassName);

        List<MethodDeclaration> candidates = compilationUnit.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName))
                .filter(method -> belongsToTargetType(method, targetSimpleName))
                .filter(method -> hasMatchingParameter(method, rootParamName, rootParamType))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Method not found: class=" + targetClassName +
                            ", method=" + methodName +
                            ", rootParamName=" + rootParamName +
                            ", rootParamType=" + rootParamType
            );
        }

        if (candidates.size() > 1) {
            throw new IllegalStateException(
                    "Ambiguous method match: class=" + targetClassName +
                            ", method=" + methodName +
                            ", rootParamName=" + rootParamName +
                            ", rootParamType=" + rootParamType +
                            ", matches=" + candidates.size()
            );
        }

        return candidates.get(0);
    }

    private boolean belongsToTargetType(MethodDeclaration method, String targetSimpleName) {
        return method.findAncestor(com.github.javaparser.ast.body.TypeDeclaration.class)
                .map(type -> type.getNameAsString().equals(targetSimpleName))
                .orElse(false);
    }

    private boolean hasMatchingParameter(MethodDeclaration method,
                                         String rootParamName,
                                         String rootParamType) {
        return method.getParameters().stream()
                .anyMatch(parameter -> matchesParameter(parameter, rootParamName, rootParamType));
    }

    private boolean matchesParameter(Parameter parameter,
                                     String rootParamName,
                                     String rootParamType) {
        if (!parameter.getNameAsString().equals(rootParamName)) {
            return false;
        }

        String actualType = normalizeTypeName(parameter.getType().asString());
        String expectedType = normalizeTypeName(rootParamType);

        if (actualType.equals(expectedType)) {
            return true;
        }

        return simpleName(actualType).equals(simpleName(expectedType));
    }

    private String normalizeTypeName(String typeName) {
        String value = typeName.trim();

        int genericIndex = value.indexOf('<');
        if (genericIndex >= 0) {
            value = value.substring(0, genericIndex);
        }

        while (value.endsWith("[]")) {
            value = value.substring(0, value.length() - 2);
        }

        return value.trim();
    }

    private String simpleName(String className) {
        String normalized = normalizeTypeName(className);
        int lastDot = normalized.lastIndexOf('.');
        return lastDot >= 0 ? normalized.substring(lastDot + 1) : normalized;
    }
}
