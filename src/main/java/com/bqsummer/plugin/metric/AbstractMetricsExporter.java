package com.bqsummer.plugin.metric;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class AbstractMetricsExporter {

    protected final String commonPrefix;

    @Getter
    protected PrometheusMeterRegistry prometheusMeterRegistry;


    public AbstractMetricsExporter() {
        this(StringUtils.EMPTY);
    }

    public AbstractMetricsExporter(final String prefix) {
        this(prefix, new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, new PrometheusRegistry(), Clock.SYSTEM));
    }

    public AbstractMetricsExporter(final String prefix, final PrometheusMeterRegistry prometheusMeterRegistry) {
        commonPrefix = prefix;
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    protected void doGauge( final String name, @Nullable final LinkedHashMap<String, String> tags, final double amount) {
        Tags micrometerTags = toTags(tags);
        prometheusMeterRegistry.gauge(name, micrometerTags, amount);
    }

    protected double doCount( final String name, @Nullable final LinkedHashMap<String, String> tags, final double deltaAmount) {
        Counter counter = registerCounterIfNecessary(name, tags);
        counter.increment(deltaAmount);
        return counter.count();
    }

    protected Counter registerCounterIfNecessary( final String name, @Nullable final LinkedHashMap<String, String> tags) {
        return prometheusMeterRegistry.counter(name, toTags(tags));
    }

    public double count( final String name, @Nullable final LinkedHashMap<String, String> tags) {
        return count(buildMetricName(name), tags, 1.0d);
    }

    public double count( final String name, @Nullable final LinkedHashMap<String, String> tags, final double deltaAmount) {
        return doCount(buildMetricName(name), tags, deltaAmount);
    }

    protected String buildMetricName( final String name) {
        return name.startsWith(commonPrefix) ? name : commonPrefix + name;
    }

    public void gauge( final String name, @Nullable final LinkedHashMap<String, String> tags, final double amount) {
        doGauge(buildMetricName(name), tags, amount);
    }

    public void gauge( final String name, final double amount) {
        doGauge(buildMetricName(name), null, amount);
    }

    public static Tags toTags(final LinkedHashMap<String, String> tagMap) {
        if (tagMap == null) {
            return Tags.of(new LinkedList<>());
        }
        return Tags.of(Collections2
                .transform(tagMap.entrySet(), entry -> Tag.of(entry.getKey(), Strings.nullToEmpty(entry.getValue()))));
    }

    public String scrape() {
        return prometheusMeterRegistry.scrape();
    }
}
