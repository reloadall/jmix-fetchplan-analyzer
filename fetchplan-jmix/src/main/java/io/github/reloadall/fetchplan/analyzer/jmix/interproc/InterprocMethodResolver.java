package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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

        List<MethodDeclaration> matches = findMatchingMethodsInHierarchy(
                targetCompilationUnit,
                concreteTargetClassName,
                methodCallExpr
        );

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

    public List<MethodDeclaration> resolveAllOnConcreteClasses(MethodCallExpr methodCallExpr,
                                                               Set<String> concreteTargetClassNames) {
        Objects.requireNonNull(methodCallExpr, "methodCallExpr is null");
        Objects.requireNonNull(concreteTargetClassNames, "concreteTargetClassNames is null");

        Set<MethodDeclaration> result = new LinkedHashSet<>();
        for (String concreteTargetClassName : concreteTargetClassNames) {
            resolveOnConcreteClass(methodCallExpr, concreteTargetClassName)
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public List<String> resolveCollectionElementImplementations(MethodDeclaration callerMethod, String scopeName) {
        Optional<ResolvedTargetType> targetTypeOpt = resolveCollectionElementTargetType(callerMethod, scopeName);
        if (targetTypeOpt.isEmpty()) {
            return List.of();
        }

        Optional<Class<?>> declaredClassOpt = tryLoadClass(targetTypeOpt.get().getDeclaredTypeName());
        if (declaredClassOpt.isEmpty()) {
            return List.of();
        }

        return springBeanImplementationResolver.resolveImplementations(declaredClassOpt.get()).stream()
                .map(Class::getName)
                .toList();
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

    private Optional<MethodDeclaration> resolveOnConcreteClass(MethodCallExpr methodCallExpr, String concreteTargetClassName) {
        Optional<CompilationUnit> targetCompilationUnitOpt = loadCompilationUnit(concreteTargetClassName);
        if (targetCompilationUnitOpt.isEmpty()) {
            return Optional.empty();
        }

        CompilationUnit targetCompilationUnit = targetCompilationUnitOpt.get();

        List<MethodDeclaration> matches = findMatchingMethodsInHierarchy(
                targetCompilationUnit,
                concreteTargetClassName,
                methodCallExpr
        );

        if (matches.size() != 1) {
            analysisTrace.log("INTERPROC: target method unresolved or ambiguous for concrete class = "
                    + concreteTargetClassName + ", call = " + methodCallExpr);
            return Optional.empty();
        }

        return Optional.of(matches.get(0));
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

    private List<MethodDeclaration> findMatchingMethodsInHierarchy(CompilationUnit targetCompilationUnit,
                                                                   String concreteTargetClassName,
                                                                   MethodCallExpr methodCallExpr) {
        return findMatchingMethodsInHierarchy(
                targetCompilationUnit,
                concreteTargetClassName,
                methodCallExpr,
                new LinkedHashSet<>()
        );
    }

    private List<MethodDeclaration> findMatchingMethodsInHierarchy(CompilationUnit compilationUnit,
                                                                   String className,
                                                                   MethodCallExpr methodCallExpr,
                                                                   Set<String> visitedClassNames) {
        if (!visitedClassNames.add(className)) {
            analysisTrace.log("INTERPROC: hierarchy skip visited class = " + className);
            return List.of();
        }

        String targetSimpleName = simpleName(className);
        analysisTrace.log("INTERPROC: hierarchy inspect class = " + className + " for call = " + methodCallExpr);
        List<MethodDeclaration> localMatches = compilationUnit.findAll(MethodDeclaration.class).stream()
                .filter(method -> belongsToTargetType(method, targetSimpleName))
                .filter(method -> method.getNameAsString().equals(methodCallExpr.getNameAsString()))
                .filter(method -> method.getParameters().size() == methodCallExpr.getArguments().size())
                .collect(Collectors.toList());
        if (!localMatches.isEmpty()) {
            analysisTrace.log("INTERPROC: hierarchy local matches in " + className + " = " + localMatches.size());
            return localMatches;
        }

        Optional<ClassOrInterfaceDeclaration> typeDeclarationOpt = compilationUnit.findFirst(
                ClassOrInterfaceDeclaration.class,
                type -> type.getNameAsString().equals(targetSimpleName)
        );
        if (typeDeclarationOpt.isEmpty()) {
            return List.of();
        }
        ClassOrInterfaceDeclaration classDeclaration = typeDeclarationOpt.get();

        for (ClassOrInterfaceType extendedType : classDeclaration.getExtendedTypes()) {
            Optional<String> superClassNameOpt = resolveTypeNameToFqcn(compilationUnit, extendedType.asString());
            if (superClassNameOpt.isEmpty()) {
                analysisTrace.log("INTERPROC: hierarchy could not resolve superclass fqcn for " + extendedType.asString());
                continue;
            }
            analysisTrace.log("INTERPROC: hierarchy try superclass = " + superClassNameOpt.get());

            if (containsTypeDeclaration(compilationUnit, simpleName(superClassNameOpt.get()))) {
                List<MethodDeclaration> sameUnitInheritedMatches = findMatchingMethodsInHierarchy(
                        compilationUnit,
                        superClassNameOpt.get(),
                        methodCallExpr,
                        visitedClassNames
                );
                if (!sameUnitInheritedMatches.isEmpty()) {
                    return sameUnitInheritedMatches;
                }
            }

            Optional<CompilationUnit> superCompilationUnitOpt = loadCompilationUnit(superClassNameOpt.get());
            if (superCompilationUnitOpt.isEmpty()) {
                analysisTrace.log("INTERPROC: hierarchy superclass source not found = " + superClassNameOpt.get());
                continue;
            }

            List<MethodDeclaration> inheritedMatches = findMatchingMethodsInHierarchy(
                    superCompilationUnitOpt.get(),
                    superClassNameOpt.get(),
                    methodCallExpr,
                    visitedClassNames
            );
            if (!inheritedMatches.isEmpty()) {
                return inheritedMatches;
            }
        }

        return List.of();
    }

    private boolean containsTypeDeclaration(CompilationUnit compilationUnit, String simpleName) {
        return compilationUnit.findFirst(
                ClassOrInterfaceDeclaration.class,
                type -> type.getNameAsString().equals(simpleName)
        ).isPresent();
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

    Optional<ResolvedTargetType> resolveCollectionElementTargetType(MethodDeclaration callerMethod, String scopeName) {
        Objects.requireNonNull(callerMethod, "callerMethod is null");
        Objects.requireNonNull(scopeName, "scopeName is null");

        CompilationUnit compilationUnit = callerMethod.findCompilationUnit().orElse(null);
        if (compilationUnit == null) {
            return Optional.empty();
        }

        TypeDeclaration<?> callerType = callerMethod.findAncestor(TypeDeclaration.class).orElse(null);
        if (callerType == null) {
            return Optional.empty();
        }

        Optional<String> declaredTypeName = resolveFieldType(callerType, scopeName)
                .or(() -> resolveParameterType(callerMethod, scopeName))
                .or(() -> resolveLocalVariableType(callerMethod, scopeName));

        if (declaredTypeName.isEmpty()) {
            return Optional.empty();
        }

        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse(declaredTypeName.get());
        if (!descriptor.isSupportedCollectionContainer()) {
            return Optional.empty();
        }

        Optional<String> elementTypeName = descriptor.getCollectionElementTypeName();
        if (elementTypeName.isEmpty()) {
            analysisTrace.log("INTERPROC: collection container without supported element type for scope = " + scopeName
                    + ", declaredType = " + declaredTypeName.get());
            return Optional.empty();
        }

        return resolveTypeNameToFqcn(compilationUnit, elementTypeName.get())
                .map(fqcn -> new ResolvedTargetType(fqcn, scopeName, true));
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
