package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_InterprocMethodResolver")
public class InterprocMethodResolver {

    private final SpringBeanImplementationResolver springBeanImplementationResolver;
    private final SourceAnalysisCache sourceAnalysisCache;
    private final AnalysisTrace analysisTrace;

    @Autowired
    public InterprocMethodResolver(SpringBeanImplementationResolver springBeanImplementationResolver,
                                   SourceAnalysisCache sourceAnalysisCache,
                                   AnalysisTrace analysisTrace) {
        this.springBeanImplementationResolver = Objects.requireNonNull(
                springBeanImplementationResolver,
                "springBeanImplementationResolver is null"
        );
        this.sourceAnalysisCache = Objects.requireNonNull(
                sourceAnalysisCache,
                "sourceAnalysisCache is null"
        );
        this.analysisTrace = Objects.requireNonNull(analysisTrace, "analysisTrace is null");
    }

    public Optional<MethodDeclaration> resolve(MethodCallExpr methodCallExpr, AnalysisStep step) {
        analysisTrace.log("INTERPROC: resolve target for call = " + methodCallExpr);
        Objects.requireNonNull(methodCallExpr, "methodCallExpr is null");
        Objects.requireNonNull(step, "step is null");

        Optional<ResolvedTargetType> targetTypeOpt = resolveTargetType(methodCallExpr, step.getMethod());
        if (targetTypeOpt.isEmpty()) {
            analysisTrace.log("INTERPROC: resolve target for call = " + methodCallExpr);
            return Optional.empty();
        }
        analysisTrace.log("INTERPROC: declared target type = " + targetTypeOpt.get().getDeclaredTypeName());
        Optional<String> concreteTargetClassNameOpt = resolveConcreteTargetClassName(targetTypeOpt.get());
        if (concreteTargetClassNameOpt.isEmpty()) {
            return Optional.empty();
        }

        String concreteTargetClassName = concreteTargetClassNameOpt.get();
        analysisTrace.log("INTERPROC: concrete target class = " + concreteTargetClassName);

        Optional<CompilationUnit> targetCompilationUnitOpt = loadCompilationUnit(concreteTargetClassName);
        if (targetCompilationUnitOpt.isEmpty()) {
            return Optional.empty();
        }

        CompilationUnit targetCompilationUnit = targetCompilationUnitOpt.get();

        String targetSimpleName = simpleName(concreteTargetClassName);

        List<MethodDeclaration> matches = targetCompilationUnit.findAll(MethodDeclaration.class).stream()
                .filter(method -> belongsToTargetType(method, targetSimpleName))
                .filter(method -> method.getNameAsString().equals(methodCallExpr.getNameAsString()))
                .filter(method -> method.getParameters().size() == methodCallExpr.getArguments().size())
                .collect(Collectors.toList());

        if (matches.size() != 1) {
            analysisTrace.log("INTERPROC: target method unresolved or ambiguous for call = " + methodCallExpr);
            return Optional.empty();
        }

        MethodDeclaration target = matches.get(0);
        analysisTrace.log("INTERPROC: target method resolved = "
                + target.findAncestor(TypeDeclaration.class).map(TypeDeclaration::getNameAsString).orElse("<unknown>")
                + "." + target.getNameAsString()
                + "(" + target.getParameters().stream().map(p -> p.getType().asString()).collect(Collectors.joining(", "))
                + ")");
        return Optional.of(target);
    }

    private Optional<CompilationUnit> loadCompilationUnit(String concreteTargetClassName) {
        Optional<java.nio.file.Path> javaFileOpt = sourceAnalysisCache.findJavaFile(concreteTargetClassName);
        if (javaFileOpt.isEmpty()) {
            analysisTrace.log("INTERPROC: source file not found for class = " + concreteTargetClassName);
            return Optional.empty();
        }

        try {
            return Optional.of(sourceAnalysisCache.getCompilationUnit(javaFileOpt.get()));
        } catch (IllegalStateException ex) {
            analysisTrace.log("INTERPROC: failed to parse source file for class = " + concreteTargetClassName);
            return Optional.empty();
        }
    }

    private Optional<String> resolveConcreteTargetClassName(ResolvedTargetType targetType) {
        Optional<Class<?>> declaredClassOpt = tryLoadClass(targetType.getDeclaredTypeName());
        if (declaredClassOpt.isEmpty()) {
            return Optional.of(targetType.getDeclaredTypeName());
        }

        Class<?> declaredClass = declaredClassOpt.get();

        if (!targetType.isSpringBeanCandidate()) {
            return Optional.of(declaredClass.getName());
        }

        Optional<Class<?>> implementationClass = springBeanImplementationResolver.resolveImplementation(
                declaredClass,
                targetType.getInjectionPointName()
        );

        return implementationClass
                .map(Class::getName)
                .or(() -> Optional.of(declaredClass.getName()));
    }

    private Optional<Class<?>> tryLoadClass(String fqcn) {
        try {
            return Optional.of(Class.forName(fqcn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<ResolvedTargetType> resolveTargetType(MethodCallExpr methodCallExpr, MethodDeclaration callerMethod) {
        CompilationUnit callerCompilationUnit = callerMethod.findCompilationUnit().orElse(null);
        if (callerCompilationUnit == null) {
            return Optional.empty();
        }

        TypeDeclaration<?> callerType = callerMethod.findAncestor(TypeDeclaration.class).orElse(null);
        if (callerType == null) {
            return Optional.empty();
        }

        if (methodCallExpr.getScope().isEmpty()) {
            return resolveOwningTypeFqcn(callerCompilationUnit, callerType)
                    .map(fqcn -> new ResolvedTargetType(fqcn, null, false));
        }

        if (methodCallExpr.getScope().get().isThisExpr()) {
            return resolveOwningTypeFqcn(callerCompilationUnit, callerType)
                    .map(fqcn -> new ResolvedTargetType(fqcn, null, false));
        }

        if (methodCallExpr.getScope().get().isNameExpr()) {
            String scopeName = methodCallExpr.getScope().get().asNameExpr().getNameAsString();
            return resolveScopedNameType(callerCompilationUnit, callerType, callerMethod, scopeName);
        }

        if (methodCallExpr.getScope().get().isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = methodCallExpr.getScope().get().asFieldAccessExpr();
            if (fieldAccessExpr.getScope().isThisExpr()) {
                return resolveScopedNameType(
                        callerCompilationUnit,
                        callerType,
                        callerMethod,
                        fieldAccessExpr.getNameAsString()
                );
            }
        }

        return Optional.empty();
    }

    private Optional<ResolvedTargetType> resolveScopedNameType(CompilationUnit compilationUnit,
                                                               TypeDeclaration<?> callerType,
                                                               MethodDeclaration callerMethod,
                                                               String scopeName) {
        Optional<String> fieldType = resolveFieldType(callerType, scopeName);
        if (fieldType.isPresent()) {
            return resolveTypeNameToFqcn(compilationUnit, fieldType.get())
                    .map(fqcn -> new ResolvedTargetType(fqcn, scopeName, true));
        }

        Optional<String> parameterType = resolveParameterType(callerMethod, scopeName);
        if (parameterType.isPresent()) {
            return resolveTypeNameToFqcn(compilationUnit, parameterType.get())
                    .map(fqcn -> new ResolvedTargetType(fqcn, scopeName, true));
        }

        Optional<String> localType = resolveLocalVariableType(callerMethod, scopeName);
        if (localType.isPresent()) {
            return resolveTypeNameToFqcn(compilationUnit, localType.get())
                    .map(fqcn -> new ResolvedTargetType(fqcn, scopeName, true));
        }

        return Optional.empty();
    }

    private Optional<String> resolveFieldType(TypeDeclaration<?> callerType, String fieldName) {
        for (FieldDeclaration fieldDeclaration : callerType.getFields()) {
            for (VariableDeclarator variable : fieldDeclaration.getVariables()) {
                if (fieldName.equals(variable.getNameAsString())) {
                    return Optional.of(variable.getType().asString());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveParameterType(MethodDeclaration callerMethod, String parameterName) {
        for (Parameter parameter : callerMethod.getParameters()) {
            if (parameterName.equals(parameter.getNameAsString())) {
                return Optional.of(parameter.getType().asString());
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveLocalVariableType(MethodDeclaration callerMethod, String variableName) {
        return callerMethod.findAll(VariableDeclarator.class).stream()
                .filter(variable -> variableName.equals(variable.getNameAsString()))
                .map(variable -> variable.getType().asString())
                .findFirst();
    }

    private Optional<String> resolveOwningTypeFqcn(CompilationUnit compilationUnit, TypeDeclaration<?> typeDeclaration) {
        String packageName = compilationUnit.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        if (packageName.isBlank()) {
            return Optional.of(typeDeclaration.getNameAsString());
        }

        return Optional.of(packageName + "." + typeDeclaration.getNameAsString());
    }

    private Optional<String> resolveTypeNameToFqcn(CompilationUnit compilationUnit, String rawTypeName) {
        String typeName = eraseGenericsAndArrays(rawTypeName);

        if (typeName.contains(".")) {
            return Optional.of(typeName);
        }

        for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
            if (!importDeclaration.isAsterisk()) {
                String importedName = importDeclaration.getNameAsString();
                if (importedName.endsWith("." + typeName)) {
                    return Optional.of(importedName);
                }
            }
        }

        String packageName = compilationUnit.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        if (packageName.isBlank()) {
            return Optional.of(typeName);
        }

        return Optional.of(packageName + "." + typeName);
    }

    private String eraseGenericsAndArrays(String typeName) {
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

    private boolean belongsToTargetType(MethodDeclaration method, String targetSimpleName) {
        return method.findAncestor(TypeDeclaration.class)
                .map(type -> type.getNameAsString().equals(targetSimpleName))
                .orElse(false);
    }

    private String simpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
}
