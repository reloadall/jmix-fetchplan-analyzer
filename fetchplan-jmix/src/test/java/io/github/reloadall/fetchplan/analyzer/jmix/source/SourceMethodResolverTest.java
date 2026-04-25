package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourceMethodResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesMethodThroughSharedSourceCache() throws Exception {
        Path sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        Path javaFile = sourceRoot.resolve("com/example/TestService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
                package com.example;

                class TestService {
                    void load(TestEntity entity) {
                        entity.getName();
                    }
                }
                """);

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        SourceMethodResolver resolver = new SourceMethodResolver(new SourceAnalysisCache(sourceRootsResolver));

        MethodDeclaration method = resolver.resolve(
                "com.example.TestService",
                "load",
                "entity",
                "com.example.TestEntity"
        );

        assertEquals("load", method.getNameAsString());
        assertEquals("entity", method.getParameter(0).getNameAsString());
    }

    @Test
    void throwsWhenJavaFileIsMissing() {
        Path sourceRoot = tempDir.resolve("src/main/java");

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        SourceMethodResolver resolver = new SourceMethodResolver(new SourceAnalysisCache(sourceRootsResolver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("com.example.MissingService", "load", "entity", "com.example.TestEntity")
        );

        assertEquals("Java source file not found for class: com.example.MissingService", exception.getMessage());
    }
}