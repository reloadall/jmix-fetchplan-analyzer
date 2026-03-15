package io.github.reloadall.fetchplan.analyzer.jmix;

import javax.sql.DataSource;

import io.jmix.core.annotation.JmixModule;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(FetchPlanAnalyzerJmixConfiguration.class)
@PropertySource("classpath:/io/github/reloadall/fetchplan/analyzer/jmix/test-app.properties")
@JmixModule(id = "io.github.reloadall.fetchplan.analyzer.jmix.test",
        dependsOn = FetchPlanAnalyzerJmixConfiguration.class)
public class FetchPlanAnalyzerJmixTestConfiguration {

    @Bean
    @Primary
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.HSQL)
                .build();
    }
}
