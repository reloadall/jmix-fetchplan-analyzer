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
    void resolvesUnqualifiedInheritedProtectedMethodFromAbstractSuperclass() throws Exception {
        Path sourceRoot = Files.createDirectories(tempDir.resolve("src/main/java"));
        Path javaFile = sourceRoot.resolve("com/example/SyntheticDocumentConverter.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
                package com.example;

                class SyntheticDto {
                }

                interface HasSyntheticDocument {
                    Agreement getAgreement();
                }

                class Agreement {
                    AgreementSides getSides() { return null; }
                }

                class AgreementSides {
                    Counterparty getCounterparty() { return null; }
                }

                class Counterparty {
                    String getName() { return null; }
                }

                class RootDocument implements HasSyntheticDocument {
                    public Agreement getAgreement() { return null; }
                }

                abstract class SyntheticBaseDtoConverter<T> {
                }

                abstract class SyntheticBaseConverter<T extends HasSyntheticDocument> extends SyntheticBaseDtoConverter<T> {
                    protected SyntheticDto createParams(HasSyntheticDocument document) {
                        document.getAgreement().getSides().getCounterparty().getName();
                        return new SyntheticDto();
                    }
                }

                class SyntheticDocumentConverter extends SyntheticBaseConverter<RootDocument> {
                    SyntheticDto createDto(RootDocument document) {
                        return createParams(document);
                    }
                }
                """);

        CompilationUnit callerUnit = StaticJavaParser.parse(javaFile);
        MethodDeclaration callerMethod = callerUnit.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("createDto"))
                .orElseThrow();

        SourceRootsResolver sourceRootsResolver = mock(SourceRootsResolver.class);
        when(sourceRootsResolver.resolveMainJavaSourceRoots()).thenReturn(List.of(sourceRoot));

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(sourceRootsResolver),
                new AnalysisTrace()
        );

        RawTree rawTree = new RawTree();
        var rootNode = rawTree.createRoot("document");
        AnalysisStep step = new AnalysisStep(
                callerMethod,
                StatementsPayload.from(callerMethod),
                rootNode,
                java.util.Map.of("document", io.github.reloadall.fetchplan.analyzer.jmix.engine.ValueBinding.of(rootNode))
        );
        Optional<MethodDeclaration> target = resolver.resolve(
                callerMethod.findFirst(com.github.javaparser.ast.expr.MethodCallExpr.class).orElseThrow(),
                step
        );

        assertTrue(target.isPresent());
        assertEquals("createParams", target.get().getNameAsString());
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

    @Test
    void resolvesListElementTypeFromFieldDeclaration() {
        MethodDeclaration callerMethod = StaticJavaParser.parse("""
                package com.example;

                import java.util.List;

                class CallerService {
                    private List<DocumentWorker> workers;

                    void caller(Document document) {
                    }
                }
                """)
                .findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(mock(SourceRootsResolver.class)),
                new AnalysisTrace()
        );

        Optional<ResolvedTargetType> targetType = resolver.resolveCollectionElementTargetType(callerMethod, "workers");

        assertTrue(targetType.isPresent());
        assertEquals("com.example.DocumentWorker", targetType.get().getDeclaredTypeName());
    }

    @Test
    void resolvesCollectionElementTypeFromParameterDeclaration() {
        MethodDeclaration callerMethod = StaticJavaParser.parse("""
                package com.example;

                import java.util.Collection;

                class CallerService {
                    void caller(Collection<DocumentWorker> workers, Document document) {
                    }
                }
                """)
                .findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(mock(SourceRootsResolver.class)),
                new AnalysisTrace()
        );

        Optional<ResolvedTargetType> targetType = resolver.resolveCollectionElementTargetType(callerMethod, "workers");

        assertTrue(targetType.isPresent());
        assertEquals("com.example.DocumentWorker", targetType.get().getDeclaredTypeName());
    }

    @Test
    void resolvesIterableElementTypeFromLocalVariableDeclaration() {
        MethodDeclaration callerMethod = StaticJavaParser.parse("""
                package com.example;

                import java.lang.Iterable;

                class CallerService {
                    void caller(Document document) {
                        Iterable<DocumentWorker> workers = null;
                    }
                }
                """)
                .findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(mock(SourceRootsResolver.class)),
                new AnalysisTrace()
        );

        Optional<ResolvedTargetType> targetType = resolver.resolveCollectionElementTargetType(callerMethod, "workers");

        assertTrue(targetType.isPresent());
        assertEquals("com.example.DocumentWorker", targetType.get().getDeclaredTypeName());
    }

    @Test
    void returnsEmptyForRawListWithoutElementType() {
        MethodDeclaration callerMethod = StaticJavaParser.parse("""
                package com.example;

                import java.util.List;

                class CallerService {
                    private List workers;

                    void caller(Document document) {
                    }
                }
                """)
                .findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("caller"))
                .orElseThrow();

        InterprocMethodResolver resolver = new InterprocMethodResolver(
                mock(SpringBeanImplementationResolver.class),
                new SourceAnalysisCache(mock(SourceRootsResolver.class)),
                new AnalysisTrace()
        );

        Optional<ResolvedTargetType> targetType = resolver.resolveCollectionElementTargetType(callerMethod, "workers");

        assertTrue(targetType.isEmpty());
    }
}