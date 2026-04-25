package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_SourceAnalysisCache")
public class SourceAnalysisCache {

    private final SourceRootsResolver sourceRootsResolver;

    private final Object sourceRootsLock = new Object();
    private volatile List<Path> mainJavaSourceRoots;

    private final ConcurrentMap<String, Optional<Path>> javaFileByClass = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, CompilationUnit> compilationUnitByPath = new ConcurrentHashMap<>();

    @Autowired
    public SourceAnalysisCache(SourceRootsResolver sourceRootsResolver) {
        this.sourceRootsResolver = Objects.requireNonNull(sourceRootsResolver, "sourceRootsResolver is null");
    }

    public List<Path> getMainJavaSourceRoots() {
        List<Path> cached = mainJavaSourceRoots;
        if (cached != null) {
            return cached;
        }

        synchronized (sourceRootsLock) {
            if (mainJavaSourceRoots == null) {
                mainJavaSourceRoots = List.copyOf(sourceRootsResolver.resolveMainJavaSourceRoots());
            }
            return mainJavaSourceRoots;
        }
    }

    public Optional<Path> findJavaFile(String targetClassName) {
        Objects.requireNonNull(targetClassName, "targetClassName is null");

        return javaFileByClass.computeIfAbsent(targetClassName, this::doFindJavaFile);
    }

    public CompilationUnit getCompilationUnit(Path javaFile) {
        Objects.requireNonNull(javaFile, "javaFile is null");

        Path normalizedPath = javaFile.toAbsolutePath().normalize();
        return compilationUnitByPath.computeIfAbsent(normalizedPath, this::parseCompilationUnit);
    }

    void clear() {
        synchronized (sourceRootsLock) {
            mainJavaSourceRoots = null;
        }
        javaFileByClass.clear();
        compilationUnitByPath.clear();
    }

    private Optional<Path> doFindJavaFile(String targetClassName) {
        String relativePath = targetClassName.replace('.', '/') + ".java";

        for (Path sourceRoot : getMainJavaSourceRoots()) {
            Path candidate = sourceRoot.resolve(relativePath).toAbsolutePath().normalize();
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private CompilationUnit parseCompilationUnit(Path javaFile) {
        try {
            return StaticJavaParser.parse(javaFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse java file: " + javaFile, e);
        }
    }
}