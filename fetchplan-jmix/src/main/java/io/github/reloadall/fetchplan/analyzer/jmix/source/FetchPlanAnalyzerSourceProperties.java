package io.github.reloadall.fetchplan.analyzer.jmix.source;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fpa.source")
public class FetchPlanAnalyzerSourceProperties {

    private boolean includeCurrentProject = true;

    private String projectsRoot;

    public boolean isIncludeCurrentProject() {
        return includeCurrentProject;
    }

    public void setIncludeCurrentProject(boolean includeCurrentProject) {
        this.includeCurrentProject = includeCurrentProject;
    }

    public String getProjectsRoot() {
        return projectsRoot;
    }

    public void setProjectsRoot(String projectsRoot) {
        this.projectsRoot = projectsRoot;
    }

    public Path getProjectsRootPath() {
        if (projectsRoot == null || projectsRoot.isBlank()) {
            return null;
        }
        return Paths.get(projectsRoot)
                .toAbsolutePath()
                .normalize();
    }
}
