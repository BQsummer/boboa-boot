package com.bqsummer.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MeterRegistryConfig {

    @Bean
    @Primary
    public PrometheusRegistry serviceCollectorRegistry() {
        return new PrometheusRegistry();
    }

    @Bean
    @Primary
    public PrometheusMeterRegistry prometheusMeterRegistry(@Qualifier("serviceCollectorRegistry") final PrometheusRegistry collectorRegistry, final MeterBinder... binders) {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM);
    }

}
