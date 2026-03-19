package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fpa_SourceRootsResolver")
public class SourceRootsResolver {

    private final FetchPlanAnalyzerSourceProperties properties;

    @Autowired
    public SourceRootsResolver(FetchPlanAnalyzerSourceProperties properties) {
        this.properties = properties;
    }

    public List<Path> resolveMainJavaSourceRoots() {
        Set<Path> result = new LinkedHashSet<>();

        if (properties.isIncludeCurrentProject()) {
            result.addAll(findMainJavaFolders(resolveCurrentProjectRoot()));
        }

        Path projectsRoot = properties.getProjectsRootPath();
        if (projectsRoot != null) {
            result.addAll(findMainJavaFoldersInProjectsRoot(projectsRoot));
        }

        return List.copyOf(result);
    }

    private Path resolveCurrentProjectRoot() {
        return Paths.get(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
    }

    private List<Path> findMainJavaFoldersInProjectsRoot(Path projectsRoot) {
        if (!Files.isDirectory(projectsRoot)) {
            throw new IllegalStateException(
                    "Configured fpa.source.projects-root is not a directory: " + projectsRoot
            );
        }

        List<Path> projectRoots = findProjectRoots(projectsRoot);
        Set<Path> result = new LinkedHashSet<>();

        for (Path projectRoot : projectRoots) {
            result.addAll(findMainJavaFolders(projectRoot));
        }

        return List.copyOf(result);
    }

    private List<Path> findProjectRoots(Path root) {
        List<Path> result = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = getFileName(dir);

                    if (isIgnoredDirectory(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (looksLikeProjectRoot(dir)) {
                        result.add(dir.toAbsolutePath().normalize());
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan projects root: " + root, e);
        }

        return result;
    }

    private List<Path> findMainJavaFolders(Path root) {
        List<Path> result = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = getFileName(dir);

                    if (isIgnoredDirectory(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (isMainJavaDirectory(dir)) {
                        result.add(dir.toAbsolutePath().normalize());
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan source roots: " + root, e);
        }

        return result;
    }

    private boolean looksLikeProjectRoot(Path dir) {
        return Files.exists(dir.resolve("pom.xml"))
                || Files.exists(dir.resolve("build.gradle"))
                || Files.exists(dir.resolve("build.gradle.kts"))
                || Files.exists(dir.resolve("settings.gradle"))
                || Files.exists(dir.resolve("settings.gradle.kts"));
    }

    private boolean isMainJavaDirectory(Path dir) {
        Path javaDir = dir.getFileName();
        if (javaDir == null || !"java".equals(javaDir.toString())) {
            return false;
        }

        Path mainDir = dir.getParent();
        if (mainDir == null || mainDir.getFileName() == null || !"main".equals(mainDir.getFileName().toString())) {
            return false;
        }

        Path srcDir = mainDir.getParent();
        return srcDir != null
                && srcDir.getFileName() != null
                && "src".equals(srcDir.getFileName().toString());
    }

    private boolean isIgnoredDirectory(String name) {
        return ".git".equals(name)
                || ".idea".equals(name)
                || ".gradle".equals(name)
                || "build".equals(name)
                || "target".equals(name)
                || "out".equals(name)
                || "node_modules".equals(name);
    }

    private String getFileName(Path path) {
        return path.getFileName() == null ? "" : path.getFileName().toString();
    }
}
