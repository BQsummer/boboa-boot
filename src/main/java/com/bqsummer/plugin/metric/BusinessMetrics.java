package com.bqsummer.plugin.metric;

import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics extends AbstractMetricsExporter {

    public static final String PREFIX = "business_";

    public BusinessMetrics() {
        super(PREFIX);
    }

}
