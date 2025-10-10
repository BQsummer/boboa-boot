package com.bqsummer.configuration;

import com.bqsummer.framework.log.CustomHttpLogWriter;
import com.bqsummer.framework.log.SimpleBodyFilter;
import com.bqsummer.framework.log.SimpleHeaderFilter;
import com.bqsummer.framework.log.SimpleHttpLogFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.core.DefaultSink;

import static org.zalando.logbook.core.Conditions.contentType;
import static org.zalando.logbook.core.Conditions.exclude;


@Configuration
public class LogbookConfiguration {
    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .headerFilter(new SimpleHeaderFilter())
                .bodyFilter(new SimpleBodyFilter())
                .condition(exclude(contentType("application/octet-stream")))
                .sink(new DefaultSink(
                    new SimpleHttpLogFormatter(),
                    new CustomHttpLogWriter()
                ))
                .build();
    }
}