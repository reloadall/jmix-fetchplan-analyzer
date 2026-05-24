package io.github.reloadall.fetchplan.analyzer.jmix;

import io.jmix.core.annotation.JmixModule;
import io.jmix.eclipselink.EclipselinkConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@ConfigurationPropertiesScan
@JmixModule(dependsOn = EclipselinkConfiguration.class)
@PropertySource(
        name = "io.github.reloadall.fetchplan.analyzer.jmix",
        value = "classpath:/io/github/reloadall/fetchplan/analyzer/jmix/module.properties"
)
public class FetchPlanAnalyzerJmixConfiguration {
}
