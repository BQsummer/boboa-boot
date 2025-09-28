package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.plugin.metric.metric.Metric;

import java.net.URL;
import java.util.List;

public class PrometheusAdapter {

    protected MetricsReporter reporter;

    protected PrometheusParser prometheusParser;

    protected PrometheusScraper scraper;

    private PrometheusAdapter() {
    }

    public PrometheusAdapter(MetricsReporter reporter, PrometheusParser prometheusParser, PrometheusScraper scraper) {
        this.reporter = reporter;
        this.prometheusParser = prometheusParser;
        this.scraper = scraper;
    }

    public void parseAndReport(URL url) {
        List<Metric> metrics = prometheusParser.parse(scraper.scrape(url));
        reporter.report(metrics);
    }

}
