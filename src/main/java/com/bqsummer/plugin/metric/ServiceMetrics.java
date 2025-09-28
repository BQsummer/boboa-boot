package com.bqsummer.plugin.metric;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ServiceMetrics extends AbstractMetricsExporter implements InitializingBean {

    @Autowired
    @Qualifier("prometheusMeterRegistry")
    protected PrometheusMeterRegistry prometheusMeterRegistry;

    @Override
    public void afterPropertiesSet() {
        super.prometheusMeterRegistry = prometheusMeterRegistry;
    }

}
