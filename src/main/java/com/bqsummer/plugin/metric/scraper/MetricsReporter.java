package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.plugin.metric.metric.Metric;

import java.util.List;

public interface MetricsReporter {
    void report(List<Metric> metrics);
}
