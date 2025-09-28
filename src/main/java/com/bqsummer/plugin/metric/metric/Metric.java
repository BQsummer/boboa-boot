package com.bqsummer.plugin.metric.metric;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Metric {

    /**
     * 指标名
     */
    @Getter
    private final String name;

    /**
     * 标签
     */
    @Getter
    private final Tags tags;

    public abstract String getMetricStr();

    private boolean calDelta = false;

    protected Metric(Builder<?> builder) {
        if (builder.name == null) {
            throw new IllegalArgumentException("Need to set name");
        }

        this.name = builder.name;
        this.tags = builder.tags;
        this.calDelta =builder.calDelta;
    }

    public boolean needCalDelta() {
        return calDelta;
    }

    public abstract static class Builder<B extends Builder<?>> {
        private String name;
        private Tags tags;
        private boolean calDelta;

        public B setName(String name) {
            this.name = name;
            return (B) this;
        }

        public B addTag(Tag tag) {
            if (tags == null) {
                tags = Tags.empty();
            }
            tags = tags.and(tag);
            return (B) this;
        }

        public B addTag(String key, String value) {
            if (tags == null) {
                tags = Tags.empty();
            }
            tags = tags.and(Tag.of(key, value));
            return (B) this;
        }

        public B addTags(List<Tag> tagList) {
            if (tags == null) {
                tags = Tags.empty();
            }
            tags = tags.and(tagList);
            return (B) this;
        }
        public B addTags(Tags tagz) {
            if (tags == null) {
                tags = Tags.empty();
            }
            tags = tags.and(tagz.stream().collect(Collectors.toList()));
            return (B) this;
        }

        public B calDelta(boolean calDelta) {
            this.calDelta = calDelta;
            return (B) this;
        }


        public abstract <T extends Metric> T build();
    }

    public static MetricType convertFromPrometheusMetricType(Meter.Type type) {
        return MetricType.valueOf(type.name());
    }
}
