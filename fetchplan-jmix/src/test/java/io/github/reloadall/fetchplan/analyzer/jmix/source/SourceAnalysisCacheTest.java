package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceAnalysisCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void cachesResolvedSourceRootsForApplicationLifetime() {
        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        List<Path> sourceRoots = List.of(tempDir.resolve("src/main/java").toAbsolutePath().normalize());
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(sourceRoots);

        SourceAnalysisCache cache = new SourceAnalysisCache(sourceRootsResolver);

        assertSame(cache.getMainJavaSourceRoots(), cache.getMainJavaSourceRoots());
        verify(sourceRootsResolver, times(1)).resolveMainJavaSourceRoots();
    }

    @Test
    void cachesResolvedJavaFileByClassName() throws Exception {
        Path sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        Path javaFile = sourceRoot.resolve("com/example/TestService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, "package com.example; class TestService {}\n");

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        SourceAnalysisCache cache = new SourceAnalysisCache(sourceRootsResolver);

        Optional<Path> first = cache.findJavaFile("com.example.TestService");
        Optional<Path> second = cache.findJavaFile("com.example.TestService");

        assertEquals(Optional.of(javaFile.toAbsolutePath().normalize()), first);
        assertEquals(first, second);
        verify(sourceRootsResolver, times(1)).resolveMainJavaSourceRoots();
    }

    @Test
    void cachesMissingJavaFileAsNegativeLookup() {
        Path sourceRoot = tempDir.resolve("src/main/java").toAbsolutePath().normalize();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        SourceAnalysisCache cache = new SourceAnalysisCache(sourceRootsResolver);

        assertFalse(cache.findJavaFile("com.example.MissingService").isPresent());
        assertFalse(cache.findJavaFile("com.example.MissingService").isPresent());
        verify(sourceRootsResolver, times(1)).resolveMainJavaSourceRoots();
    }

    @Test
    void cachesParsedCompilationUnitByFilePath() throws Exception {
        Path javaFile = tempDir.resolve("Sample.java");
        Files.writeString(javaFile, "class Sample {}\n");

        SourceAnalysisCache cache = new SourceAnalysisCache(mock(SourceRootsResolver.class));

        CompilationUnit first = cache.getCompilationUnit(javaFile);
        CompilationUnit second = cache.getCompilationUnit(javaFile);

        assertNotNull(first);
        assertSame(first, second);
    }
}