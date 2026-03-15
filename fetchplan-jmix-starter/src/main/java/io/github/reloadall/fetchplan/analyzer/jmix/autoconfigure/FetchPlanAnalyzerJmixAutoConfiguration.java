package io.github.reloadall.fetchplan.analyzer.jmix.autoconfigure;

import io.github.reloadall.fetchplan.analyzer.jmix.FetchPlanAnalyzerJmixConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({FetchPlanAnalyzerJmixConfiguration.class})
public class FetchPlanAnalyzerJmixAutoConfiguration {
}

