package io.github.reloadall.fetchplan.analyzer.jmix;

import io.github.reloadall.fetchplan.analyzer.jmix.report.SingleMethodAnalysisReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "main.liquibase.enabled=false")
class FetchPlanAnalyzerJmixTest {

	@Autowired
	SingleMethodAnalysisReportService singleMethodAnalysisReportService;

	@Test
	void contextLoads() {
	}

	@Test
	void singleMethodAnalysisReportServiceIsAvailableAsSpringBean() {
		assertNotNull(singleMethodAnalysisReportService);
	}
}
