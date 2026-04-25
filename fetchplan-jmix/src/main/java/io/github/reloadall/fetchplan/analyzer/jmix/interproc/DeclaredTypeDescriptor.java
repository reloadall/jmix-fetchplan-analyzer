package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.util.Objects;
import java.util.Optional;

public class DeclaredTypeDescriptor {

    private final String rawDeclaredTypeName;
    private final String rawTypeName;
    private final boolean supportedCollectionContainer;
    private final String collectionElementTypeName;

    private DeclaredTypeDescriptor(String rawDeclaredTypeName,
                                   String rawTypeName,
                                   boolean supportedCollectionContainer,
                                   String collectionElementTypeName) {
        this.rawDeclaredTypeName = Objects.requireNonNull(rawDeclaredTypeName, "rawDeclaredTypeName is null");
        this.rawTypeName = Objects.requireNonNull(rawTypeName, "rawTypeName is null");
        this.supportedCollectionContainer = supportedCollectionContainer;
        this.collectionElementTypeName = collectionElementTypeName;
    }

    public static DeclaredTypeDescriptor parse(String declaredTypeName) {
        Objects.requireNonNull(declaredTypeName, "declaredTypeName is null");

        String rawDeclaredTypeName = declaredTypeName.trim();
        String rawTypeName = eraseGenericsAndArrays(rawDeclaredTypeName);
        boolean supportedCollectionContainer = isSupportedCollectionContainer(rawTypeName);

        if (!supportedCollectionContainer) {
            return new DeclaredTypeDescriptor(rawDeclaredTypeName, rawTypeName, false, null);
        }

        String collectionElementTypeName = extractSupportedCollectionElementType(rawDeclaredTypeName).orElse(null);
        return new DeclaredTypeDescriptor(rawDeclaredTypeName, rawTypeName, true, collectionElementTypeName);
    }

    public String getRawDeclaredTypeName() {
        return rawDeclaredTypeName;
    }

    public String getRawTypeName() {
        return rawTypeName;
    }

    public boolean isSupportedCollectionContainer() {
        return supportedCollectionContainer;
    }

    public Optional<String> getCollectionElementTypeName() {
        return Optional.ofNullable(collectionElementTypeName);
    }

    private static Optional<String> extractSupportedCollectionElementType(String declaredTypeName) {
        int genericStart = declaredTypeName.indexOf('<');
        int genericEnd = declaredTypeName.lastIndexOf('>');
        if (genericStart < 0 || genericEnd <= genericStart) {
            return Optional.empty();
        }

        String genericContent = declaredTypeName.substring(genericStart + 1, genericEnd).trim();
        if (genericContent.isBlank()) {
            return Optional.empty();
        }

        if (genericContent.contains(",") || genericContent.contains("?") || genericContent.contains("<") || genericContent.contains(">")) {
            return Optional.empty();
        }

        if (genericContent.endsWith("[]")) {
            return Optional.empty();
        }

        return Optional.of(genericContent);
    }

    private static boolean isSupportedCollectionContainer(String rawTypeName) {
        return "List".equals(rawTypeName)
                || "Collection".equals(rawTypeName)
                || "Iterable".equals(rawTypeName)
                || "java.util.List".equals(rawTypeName)
                || "java.util.Collection".equals(rawTypeName)
                || "java.lang.Iterable".equals(rawTypeName);
    }

    private static String eraseGenericsAndArrays(String typeName) {
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
}