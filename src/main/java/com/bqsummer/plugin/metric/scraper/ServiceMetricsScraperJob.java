package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.framework.job.JobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;

@Slf4j
//@JobInfo(jobName = "ServiceMetricsScraperJob", cron = "0 0/1 * * * ?")
public class ServiceMetricsScraperJob extends JobExecutor {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    @Qualifier("prometheusAdapter")
    public PrometheusAdapter prometheusAdapter;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            prometheusAdapter.parseAndReport(new URL("http", "127.0.0.1", 9099, "/" + appName + "/actuator/prometheus"));
        } catch (Exception e) {
            log.error("service prometheus metrics report failed.", e);
        }
    }
}
