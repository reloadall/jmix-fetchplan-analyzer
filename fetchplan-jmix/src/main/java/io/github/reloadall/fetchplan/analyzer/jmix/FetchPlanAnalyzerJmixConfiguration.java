package io.github.reloadall.fetchplan.analyzer.jmix;

import java.util.Collections;

import io.jmix.core.annotation.JmixModule;
import io.jmix.core.impl.scanning.AnnotationScanMetadataReaderFactory;
import io.jmix.eclipselink.EclipselinkConfiguration;
import io.jmix.ui.UiConfiguration;
import io.jmix.ui.sys.ActionsConfiguration;
import io.jmix.ui.sys.UiControllersConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan
@ConfigurationPropertiesScan
@JmixModule(dependsOn = {EclipselinkConfiguration.class, UiConfiguration.class})
@PropertySource(
        name = "io.github.reloadall.fetchplan.analyzer.jmix",
        value = "classpath:/io/github/reloadall/fetchplan/analyzer/jmix/module.properties"
)
public class FetchPlanAnalyzerJmixConfiguration {

    private static final String BASE_PACKAGE = "io.github.reloadall.fetchplan.analyzer.jmix";

    @Bean("fetchPlanAnalyzerUiControllers")
    public UiControllersConfiguration uiControllers(
            ApplicationContext applicationContext,
            AnnotationScanMetadataReaderFactory metadataReaderFactory) {

        UiControllersConfiguration uiControllers =
                new UiControllersConfiguration(applicationContext, metadataReaderFactory);
        uiControllers.setBasePackages(Collections.singletonList(BASE_PACKAGE));
        return uiControllers;
    }

    @Bean("fetchPlanAnalyzerUiActions")
    public ActionsConfiguration uiActions(
            ApplicationContext applicationContext,
            AnnotationScanMetadataReaderFactory metadataReaderFactory) {

        ActionsConfiguration actionsConfiguration =
                new ActionsConfiguration(applicationContext, metadataReaderFactory);
        actionsConfiguration.setBasePackages(Collections.singletonList(BASE_PACKAGE));
        return actionsConfiguration;
    }
}
