package io.github.reloadall.fetchplan.analyzer.jmix.engine.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;

public class GetterPropertyAccessResolver {

    private static final int MAX_COMPUTED_GETTER_DEPTH = 4;

    private final SourceAnalysisCache sourceAnalysisCache;

    public GetterPropertyAccessResolver() {
        this(null);
    }

    public GetterPropertyAccessResolver(SourceAnalysisCache sourceAnalysisCache) {
        this.sourceAnalysisCache = sourceAnalysisCache;
    }

    public Set<String> resolvePropertyNames(MethodCallExpr methodCallExpr, MethodDeclaration currentMethod) {
        Objects.requireNonNull(methodCallExpr, "methodCallExpr is null");
        Objects.requireNonNull(currentMethod, "currentMethod is null");

        String candidateProperty = extractGetterPropertyName(methodCallExpr);
        if (candidateProperty == null) {
            return Set.of();
        }

        if (methodCallExpr.getScope().isEmpty()) {
            return Set.of(candidateProperty);
        }

        Optional<Class<?>> scopeType = resolveExpressionType(currentMethod, methodCallExpr.getScope().get());
        if (scopeType.isEmpty()) {
            return Set.of(candidateProperty);
        }

        Class<?> scopeClass = scopeType.get();
        if (!isEntityLikeClass(scopeClass)) {
            return Set.of(candidateProperty);
        }

        if (hasBackedProperty(scopeClass, candidateProperty)) {
            return Set.of(candidateProperty);
        }

        return analyzeComputedGetterBody(scopeClass, methodCallExpr.getNameAsString());
    }

    public Optional<String> resolveBackedPropertyName(MethodDeclaration currentMethod,
                                                      String ownerTypeName,
                                                      String methodName) {
        Objects.requireNonNull(currentMethod, "currentMethod is null");

        String candidateProperty = extractGetterPropertyName(methodName);
        if (candidateProperty == null) {
            return Optional.empty();
        }

        return resolveTypeName(currentMethod, ownerTypeName)
                .filter(this::isEntityLikeClass)
                .filter(ownerType -> hasBackedProperty(ownerType, candidateProperty))
                .map(ownerType -> candidateProperty);
    }

    private Set<String> analyzeComputedGetterBody(Class<?> entityClass, String methodName) {
        if (sourceAnalysisCache == null) {
            return Set.of();
        }

        Optional<MethodDeclaration> sourceMethod = resolveZeroArgSourceMethod(entityClass, methodName);
        if (sourceMethod.isEmpty()) {
            return Set.of();
        }

        return analyzeMethodBody(entityClass, sourceMethod.get(), new ArrayDeque<>(), 0);
    }

    private Set<String> analyzeMethodBody(Class<?> entityClass,
                                          MethodDeclaration methodDeclaration,
                                          Deque<String> callStack,
                                          int depth) {
        if (depth > MAX_COMPUTED_GETTER_DEPTH || methodDeclaration.getBody().isEmpty()) {
            return Set.of();
        }

        String recursionKey = entityClass.getName() + "#" + methodDeclaration.getNameAsString();
        if (callStack.contains(recursionKey)) {
            return Set.of();
        }

        callStack.addLast(recursionKey);
        try {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            LinkedHashMap<String, Set<String>> locals = new LinkedHashMap<>();
            for (Statement statement : methodDeclaration.getBody().get().getStatements()) {
                analyzeStatement(entityClass, statement, locals, result, callStack, depth);
            }
            return Set.copyOf(result);
        } finally {
            callStack.removeLast();
        }
    }

    private void analyzeStatement(Class<?> entityClass,
                                  Statement statement,
                                  Map<String, Set<String>> locals,
                                  Set<String> result,
                                  Deque<String> callStack,
                                  int depth) {
        if (statement.isBlockStmt()) {
            analyzeBlock(entityClass, statement.asBlockStmt(), new LinkedHashMap<>(locals), result, callStack, depth);
            return;
        }

        if (statement.isReturnStmt()) {
            ReturnStmt returnStmt = statement.asReturnStmt();
            returnStmt.getExpression().ifPresent(expr -> result.addAll(analyzeExpression(entityClass, expr, locals, callStack, depth)));
            return;
        }

        if (statement.isExpressionStmt()) {
            analyzeExpressionStatement(entityClass, statement.asExpressionStmt(), locals, result, callStack, depth);
            return;
        }

        if (statement.isIfStmt()) {
            IfStmt ifStmt = statement.asIfStmt();
            result.addAll(analyzeExpression(entityClass, ifStmt.getCondition(), locals, callStack, depth));
            analyzeStatement(entityClass, ifStmt.getThenStmt(), new LinkedHashMap<>(locals), result, callStack, depth);
            ifStmt.getElseStmt().ifPresent(elseStmt ->
                    analyzeStatement(entityClass, elseStmt, new LinkedHashMap<>(locals), result, callStack, depth));
            return;
        }

        if (statement.isForEachStmt()) {
            ForEachStmt forEachStmt = statement.asForEachStmt();
            result.addAll(analyzeExpression(entityClass, forEachStmt.getIterable(), locals, callStack, depth));
            analyzeStatement(entityClass, forEachStmt.getBody(), new LinkedHashMap<>(locals), result, callStack, depth);
        }
    }

    private void analyzeBlock(Class<?> entityClass,
                              BlockStmt blockStmt,
                              Map<String, Set<String>> locals,
                              Set<String> result,
                              Deque<String> callStack,
                              int depth) {
        for (Statement nested : blockStmt.getStatements()) {
            analyzeStatement(entityClass, nested, locals, result, callStack, depth);
        }
    }

    private void analyzeExpressionStatement(Class<?> entityClass,
                                            ExpressionStmt expressionStmt,
                                            Map<String, Set<String>> locals,
                                            Set<String> result,
                                            Deque<String> callStack,
                                            int depth) {
        Expression expression = expressionStmt.getExpression();

        if (expression.isVariableDeclarationExpr()) {
            VariableDeclarationExpr declarationExpr = expression.asVariableDeclarationExpr();
            for (VariableDeclarator variable : declarationExpr.getVariables()) {
                Set<String> properties = variable.getInitializer()
                        .map(initializer -> analyzeExpression(entityClass, initializer, locals, callStack, depth))
                        .orElse(Set.of());
                locals.put(variable.getNameAsString(), properties);
                result.addAll(properties);
            }
            return;
        }

        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            Set<String> properties = analyzeExpression(entityClass, assignExpr.getValue(), locals, callStack, depth);
            if (assignExpr.getTarget().isNameExpr()) {
                locals.put(assignExpr.getTarget().asNameExpr().getNameAsString(), properties);
            }
            result.addAll(properties);
            return;
        }

        result.addAll(analyzeExpression(entityClass, expression, locals, callStack, depth));
    }

    private Set<String> analyzeExpression(Class<?> entityClass,
                                          Expression expression,
                                          Map<String, Set<String>> locals,
                                          Deque<String> callStack,
                                          int depth) {
        if (expression == null || expression instanceof LiteralExpr) {
            return Set.of();
        }

        if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            return analyzeExpression(entityClass, enclosedExpr.getInner(), locals, callStack, depth);
        }

        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            return analyzeExpression(entityClass, castExpr.getExpression(), locals, callStack, depth);
        }

        if (expression.isConditionalExpr()) {
            ConditionalExpr conditionalExpr = expression.asConditionalExpr();
            return union(
                    analyzeExpression(entityClass, conditionalExpr.getCondition(), locals, callStack, depth),
                    analyzeExpression(entityClass, conditionalExpr.getThenExpr(), locals, callStack, depth),
                    analyzeExpression(entityClass, conditionalExpr.getElseExpr(), locals, callStack, depth)
            );
        }

        if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            return union(
                    analyzeExpression(entityClass, binaryExpr.getLeft(), locals, callStack, depth),
                    analyzeExpression(entityClass, binaryExpr.getRight(), locals, callStack, depth)
            );
        }

        if (expression.isNameExpr()) {
            return analyzeNameExpr(entityClass, expression.asNameExpr(), locals);
        }

        if (expression.isFieldAccessExpr()) {
            return analyzeFieldAccessExpr(entityClass, expression.asFieldAccessExpr(), locals);
        }

        if (expression.isMethodCallExpr()) {
            return analyzeMethodCallExpr(entityClass, expression.asMethodCallExpr(), locals, callStack, depth);
        }

        return Set.of();
    }

    private Set<String> analyzeNameExpr(Class<?> entityClass,
                                        NameExpr nameExpr,
                                        Map<String, Set<String>> locals) {
        Set<String> bound = locals.get(nameExpr.getNameAsString());
        if (bound != null) {
            return bound;
        }

        return hasBackedProperty(entityClass, nameExpr.getNameAsString())
                ? Set.of(nameExpr.getNameAsString())
                : Set.of();
    }

    private Set<String> analyzeFieldAccessExpr(Class<?> entityClass,
                                               FieldAccessExpr fieldAccessExpr,
                                               Map<String, Set<String>> locals) {
        if (fieldAccessExpr.getScope().isThisExpr() && hasBackedProperty(entityClass, fieldAccessExpr.getNameAsString())) {
            return Set.of(fieldAccessExpr.getNameAsString());
        }

        if (fieldAccessExpr.getScope().isNameExpr()) {
            Set<String> bound = locals.get(fieldAccessExpr.getScope().asNameExpr().getNameAsString());
            if (bound != null) {
                return bound;
            }
        }

        return Set.of();
    }

    private Set<String> analyzeMethodCallExpr(Class<?> entityClass,
                                              MethodCallExpr methodCallExpr,
                                              Map<String, Set<String>> locals,
                                              Deque<String> callStack,
                                              int depth) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        for (Expression argument : methodCallExpr.getArguments()) {
            result.addAll(analyzeExpression(entityClass, argument, locals, callStack, depth));
        }

        boolean sameEntityCall = methodCallExpr.getScope().isEmpty()
                || methodCallExpr.getScope().get() instanceof ThisExpr;

        if (!sameEntityCall && methodCallExpr.getScope().isPresent()) {
            result.addAll(analyzeExpression(entityClass, methodCallExpr.getScope().get(), locals, callStack, depth));
            return Set.copyOf(result);
        }

        String candidateProperty = extractGetterPropertyName(methodCallExpr);
        if (candidateProperty == null) {
            return Set.copyOf(result);
        }

        if (hasBackedProperty(entityClass, candidateProperty)) {
            result.add(candidateProperty);
            return Set.copyOf(result);
        }

        resolveZeroArgSourceMethod(entityClass, methodCallExpr.getNameAsString())
                .ifPresent(method -> result.addAll(analyzeMethodBody(entityClass, method, callStack, depth + 1)));
        return Set.copyOf(result);
    }

    private Optional<MethodDeclaration> resolveZeroArgSourceMethod(Class<?> entityClass, String methodName) {
        if (sourceAnalysisCache == null) {
            return Optional.empty();
        }

        Optional<Path> javaFile = sourceAnalysisCache.findJavaFile(entityClass.getName());
        if (javaFile.isEmpty()) {
            return Optional.empty();
        }

        CompilationUnit compilationUnit = sourceAnalysisCache.getCompilationUnit(javaFile.get());
        String simpleName = entityClass.getSimpleName();

        return compilationUnit.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName))
                .filter(method -> method.getParameters().isEmpty())
                .filter(method -> method.findAncestor(TypeDeclaration.class)
                        .map(type -> type.getNameAsString().equals(simpleName))
                        .orElse(false))
                .findFirst();
    }

    private Set<String> union(Set<String>... parts) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Set<String> part : parts) {
            if (part != null) {
                result.addAll(part);
            }
        }
        return Set.copyOf(result);
    }

    private String extractGetterPropertyName(MethodCallExpr methodCallExpr) {
        if (!methodCallExpr.getArguments().isEmpty()) {
            return null;
        }

        return extractGetterPropertyName(methodCallExpr.getNameAsString());
    }

    private String extractGetterPropertyName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }

        return null;
    }

    private Optional<Class<?>> resolveExpressionType(MethodDeclaration currentMethod, Expression expression) {
        if (expression == null) {
            return Optional.empty();
        }

        if (expression.isNameExpr()) {
            return resolveNameType(currentMethod, expression.asNameExpr());
        }

        if (expression.isThisExpr()) {
            return resolveOwningType(currentMethod);
        }

        if (expression.isFieldAccessExpr()) {
            return resolveFieldAccessType(currentMethod, expression.asFieldAccessExpr());
        }

        if (expression.isMethodCallExpr()) {
            return resolveMethodCallType(currentMethod, expression.asMethodCallExpr());
        }

        if (expression.isEnclosedExpr()) {
            EnclosedExpr enclosedExpr = expression.asEnclosedExpr();
            return resolveExpressionType(currentMethod, enclosedExpr.getInner());
        }

        if (expression.isCastExpr()) {
            CastExpr castExpr = expression.asCastExpr();
            return resolveTypeName(currentMethod, castExpr.getType().asString())
                    .or(() -> resolveExpressionType(currentMethod, castExpr.getExpression()));
        }

        return Optional.empty();
    }

    private Optional<Class<?>> resolveNameType(MethodDeclaration currentMethod, NameExpr nameExpr) {
        String name = nameExpr.getNameAsString();

        for (Parameter parameter : currentMethod.getParameters()) {
            if (name.equals(parameter.getNameAsString())) {
                return resolveTypeName(currentMethod, parameter.getType().asString());
            }
        }

        Optional<Class<?>> localType = currentMethod.findAll(VariableDeclarator.class).stream()
                .filter(variable -> name.equals(variable.getNameAsString()))
                .findFirst()
                .flatMap(variable -> resolveTypeName(currentMethod, variable.getType().asString()));
        if (localType.isPresent()) {
            return localType;
        }

        TypeDeclaration<?> ownerType = currentMethod.findAncestor(TypeDeclaration.class).orElse(null);
        if (ownerType == null) {
            return Optional.empty();
        }

        for (FieldDeclaration fieldDeclaration : ownerType.getFields()) {
            for (VariableDeclarator variable : fieldDeclaration.getVariables()) {
                if (name.equals(variable.getNameAsString())) {
                    return resolveTypeName(currentMethod, variable.getType().asString());
                }
            }
        }

        return Optional.empty();
    }

    private Optional<Class<?>> resolveFieldAccessType(MethodDeclaration currentMethod, FieldAccessExpr fieldAccessExpr) {
        if (fieldAccessExpr.getScope().isThisExpr()) {
            TypeDeclaration<?> ownerType = currentMethod.findAncestor(TypeDeclaration.class).orElse(null);
            if (ownerType == null) {
                return Optional.empty();
            }

            for (FieldDeclaration fieldDeclaration : ownerType.getFields()) {
                for (VariableDeclarator variable : fieldDeclaration.getVariables()) {
                    if (fieldAccessExpr.getNameAsString().equals(variable.getNameAsString())) {
                        return resolveTypeName(currentMethod, variable.getType().asString());
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Optional<Class<?>> resolveMethodCallType(MethodDeclaration currentMethod, MethodCallExpr methodCallExpr) {
        if (methodCallExpr.getScope().isEmpty()) {
            return resolveOwningType(currentMethod)
                    .flatMap(ownerType -> findMethod(ownerType, methodCallExpr.getNameAsString(), methodCallExpr.getArguments().size()))
                    .map(Method::getReturnType);
        }

        Optional<Class<?>> ownerType = resolveExpressionType(currentMethod, methodCallExpr.getScope().get());
        if (ownerType.isEmpty()) {
            return Optional.empty();
        }

        return findMethod(ownerType.get(), methodCallExpr.getNameAsString(), methodCallExpr.getArguments().size())
                .map(Method::getReturnType);
    }

    private Optional<Method> findMethod(Class<?> ownerType, String methodName, int argumentCount) {
        for (Method method : ownerType.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argumentCount) {
                return Optional.of(method);
            }
        }

        Class<?> current = ownerType;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == argumentCount) {
                    return Optional.of(method);
                }
            }
            current = current.getSuperclass();
        }

        return Optional.empty();
    }

    private Optional<Class<?>> resolveOwningType(MethodDeclaration currentMethod) {
        CompilationUnit compilationUnit = currentMethod.findCompilationUnit().orElse(null);
        TypeDeclaration<?> ownerType = currentMethod.findAncestor(TypeDeclaration.class).orElse(null);
        if (compilationUnit == null || ownerType == null) {
            return Optional.empty();
        }

        String packageName = compilationUnit.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");
        String fqcn = packageName.isBlank()
                ? ownerType.getNameAsString()
                : packageName + "." + ownerType.getNameAsString();
        return tryLoadClass(fqcn);
    }

    private Optional<Class<?>> resolveTypeName(MethodDeclaration currentMethod, String rawTypeName) {
        String typeName = eraseGenericsAndArrays(rawTypeName);
        if (typeName.isBlank() || isPrimitive(typeName)) {
            return Optional.empty();
        }

        if (typeName.contains(".")) {
            return tryLoadClass(typeName);
        }

        CompilationUnit compilationUnit = currentMethod.findCompilationUnit().orElse(null);
        if (compilationUnit == null) {
            return tryLoadClass(typeName).or(() -> tryLoadJavaLangClass(typeName));
        }

        for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
            if (importDeclaration.isAsterisk()) {
                continue;
            }

            String importedName = importDeclaration.getNameAsString();
            if (importedName.endsWith("." + typeName)) {
                Optional<Class<?>> importedClass = tryLoadClass(importedName);
                if (importedClass.isPresent()) {
                    return importedClass;
                }
            }
        }

        String packageName = compilationUnit.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");
        if (!packageName.isBlank()) {
            Optional<Class<?>> samePackageClass = tryLoadClass(packageName + "." + typeName);
            if (samePackageClass.isPresent()) {
                return samePackageClass;
            }
        }

        return tryLoadClass(typeName).or(() -> tryLoadJavaLangClass(typeName));
    }

    private Optional<Class<?>> tryLoadClass(String fqcn) {
        try {
            return Optional.of(Class.forName(fqcn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<Class<?>> tryLoadJavaLangClass(String simpleName) {
        return tryLoadClass("java.lang." + simpleName);
    }

    private boolean isEntityLikeClass(Class<?> candidate) {
        return hasAnnotation(candidate, "io.jmix.core.metamodel.annotation.JmixEntity")
                || hasAnnotation(candidate, "javax.persistence.Entity");
    }

    private boolean hasBackedProperty(Class<?> entityClass, String propertyName) {
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.getName().equals(propertyName)) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                if (hasAnnotation(field, "javax.persistence.Transient")) {
                    continue;
                }
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean hasAnnotation(Class<?> candidate, String annotationClassName) {
        return candidate.getAnnotations().length > 0
                && java.util.Arrays.stream(candidate.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().equals(annotationClassName));
    }

    private boolean hasAnnotation(Field field, String annotationClassName) {
        return java.util.Arrays.stream(field.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().equals(annotationClassName));
    }

    private boolean isPrimitive(String typeName) {
        return switch (typeName) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> true;
            default -> false;
        };
    }

    private String eraseGenericsAndArrays(String typeName) {
        String value = typeName == null ? "" : typeName.trim();
        int genericIndex = value.indexOf('<');
        if (genericIndex >= 0) {
            value = value.substring(0, genericIndex);
        }
        while (value.endsWith("[]")) {
            value = value.substring(0, value.length() - 2);
        }
        return value.trim();
    }

    private String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toLowerCase();
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}