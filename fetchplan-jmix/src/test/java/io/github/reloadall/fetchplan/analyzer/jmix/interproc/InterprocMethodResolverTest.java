package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.reloadall.fetchplan.analyzer.jmix.debug.AnalysisTrace;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.AnalysisStep;
import io.github.reloadall.fetchplan.analyzer.jmix.engine.StatementsPayload;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceAnalysisCache;
import io.github.reloadall.fetchplan.analyzer.jmix.source.SourceRootsResolver;
import io.github.reloadall.fetchplan.analyzer.jmix.tree.RawTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterprocMethodResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesSameClassMethodUsingSharedSourceCache() throws Exception {
        Path sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        Path javaFile = sourceRoot.resolve("com/example/TestService.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
                package com.example;

                class TestService {
                    void caller(TestEntity entity) {
                        helper(entity);
                    }

                    void helper(TestEntity entity) {
                        entity.getName();
                    }
                }
                """);

        CompilationUnit callerUnit = StaticJavaParser.parse(javaFile);
        MethodDeclaration callerMethod = callerUnit.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(sourceRootsResolver),
                new AnalysisTrace()
        );

        RawTree rawTree = new RawTree();
        var rootNode = rawTree.createRoot("entity");
        AnalysisStep step = new AnalysisStep(
                callerMethod,
                StatementsPayload.from(callerMethod),
                rootNode,
                java.util.Map.of("entity", io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding.of(rootNode))
        );
        Optional<MethodDeclaration> target = resolver.resolve(
                callerMethod.findFirst(com.github.javaparser.ast.expr.MethodCallExpr.class).orElseThrow(),
                step
        );

        assertTrue(target.isPresent());
        assertEquals("helper", target.get().getNameAsString());
    }

    @Test
    void returnsEmptyWhenTargetSourceFileIsMissing() {
        CompilationUnit callerUnit = StaticJavaParser.parse("""
                package com.example;

                class CallerService {
                    void caller(TestService service, TestEntity entity) {
                        service.helper(entity);
                    }
                }
                """);
        MethodDeclaration callerMethod = callerUnit.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(tempDir.resolve("src/main/java")));

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(sourceRootsResolver),
                new AnalysisTrace()
        );

        RawTree rawTree = new RawTree();
        var rootNode = rawTree.createRoot("entity");
        AnalysisStep step = new AnalysisStep(
                callerMethod,
                StatementsPayload.from(callerMethod),
                rootNode,
                java.util.Map.of("entity", io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding.of(rootNode))
        );
        Optional<MethodDeclaration> target = resolver.resolve(
                callerMethod.findFirst(com.github.javaparser.ast.expr.MethodCallExpr.class).orElseThrow(),
                step
        );

        assertTrue(target.isEmpty());
    }
}