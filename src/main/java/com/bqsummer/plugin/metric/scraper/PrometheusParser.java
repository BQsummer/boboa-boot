package com.bqsummer.plugin.metric.scraper;

import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.plugin.metric.metric.Counter;
import com.bqsummer.plugin.metric.metric.Gauge;
import com.bqsummer.plugin.metric.metric.Metric;
import com.bqsummer.plugin.metric.metric.MetricType;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.bqsummer.constant.ErrorEnum.PROMETHEUS_METRICS_PARSE_FAILED;
import static com.bqsummer.constant.ErrorEnum.PROMETHEUS_METRIC_TYPE_ERROR;

@Slf4j
public class PrometheusParser {

    public static final String HASH_TAG_HELP = "# HELP";

    public static final String HASH_TAG_TYPE = "# TYPE";

    public static final String LEFT_BRACES = "{";

    public static final String RIGHT_BRACES = "}";

    public static final String COMMA = ",";

    public static final String EQUAL = "=";

    public static final String QUOTE = "\"";

    private List<Metric> result;

    private MetricType type;

    private MetricsCountCache cache;

    public PrometheusParser(MetricsCountCache cache) {
        this.cache = cache;
    }

    public List<Metric> parse(String endpointText) {
        result = new ArrayList<>();
        String[] lines = endpointText.split("[\n\r]");
        if (lines.length > 0) {
            for (String line : lines) {
                try {
                    parseLine(line);
                } catch (Exception e) {
                    log.error("parse line failed. line = {}", line, e);
                }
            }
        }
        return result;
    }

    private void parseLine(String line) {
        if(StringUtils.isBlank(line)) {
            return;
        }
        if (StringUtils.startsWith(line, HASH_TAG_HELP)) {
            parseHelpLine(line);
        } else if (StringUtils.startsWith(line, HASH_TAG_TYPE)) {
            parseTypeLine(line);
        } else if(StringUtils.contains(line, LEFT_BRACES)) {
            parseMetricsLineWithLabels(line);
        } else {
            parseMetricsLineWithoutLabels(line);
        }
    }

    private void parseHelpLine(String line) {
        // doNothing
    }

    private void parseTypeLine(String line) {
        String[] ele = line.split("\\s");
        if (ele.length < 4) {
            throw new SnorlaxClientException(PROMETHEUS_METRICS_PARSE_FAILED.getCode(), PROMETHEUS_METRICS_PARSE_FAILED.getMessage());
        }
        type = identifyMetricType(ele[3]);
    }

    private MetricType identifyMetricType(String metricTypeStr) {
        return MetricType.valueOf(metricTypeStr.toUpperCase());
    }

    private void parseMetricsLineWithoutLabels(String line) {
        String[] ele = line.split("\\s");
        String metricName = parseMetricName(ele[0]);
        Double value = parseValue(ele[1]);
        saveMeter(buildMetric(metricName, Tags.empty(), value));
    }

    private void parseMetricsLineWithLabels(String line) {
        String[] ele = line.split("}\\s");
        String metricName = parseMetricName(ele[0]);
        Tags tags = parseMetricLabels(ele[0]);
        Double value = parseValue(ele[1]);
        saveMeter(buildMetric(metricName, tags, value));
    }

    private Metric buildMetric(String metricName, Tags tags, Double value) {
        switch (type) {
            case COUNTER: return new Counter.Builder().setName(metricName).addTags(tags).setValue(value).calDelta(true).build();
            case GAUGE: return new Gauge.Builder().setName(metricName).addTags(tags).setValue(value).calDelta(false).build();
            case SUMMARY: return new Gauge.Builder().setName(metricName).addTags(tags).setValue(value).calDelta(true).build();
            default: throw new SnorlaxClientException(PROMETHEUS_METRIC_TYPE_ERROR.getCode(), String.format(PROMETHEUS_METRIC_TYPE_ERROR.getMessage(), type));
        }
    }

    private String parseMetricName(String metricNameAndLabels) {
        if (metricNameAndLabels.contains(LEFT_BRACES)) {
            return metricNameAndLabels.substring(0, metricNameAndLabels.indexOf(LEFT_BRACES));
        }else {
            return metricNameAndLabels.trim();
        }
    }

    private Tags parseMetricLabels(String metricNameAndLabels) {
        String labelStr = metricNameAndLabels.substring(metricNameAndLabels.indexOf(LEFT_BRACES) + 1);
        String[] labels = labelStr.split(COMMA);
        if (labels.length > 0) {
            List<Tag> tags = Arrays.stream(labels).map(this::parseTag).collect(Collectors.toList());
            return Tags.of(tags);
        } else {
            return Tags.empty();
        }
    }

    public Tag parseTag(String label) {
        String[] keyAndValue = label.split(EQUAL);
        return Tag.of(keyAndValue[0], removeAroundQuotes(keyAndValue[1]));
    }

    public String removeAroundQuotes(String str) {
        String trim = str.trim();
        if (str.startsWith(QUOTE)) {
            trim = trim.substring(1);
        }
        if (trim.endsWith(QUOTE)) {
            trim = trim.substring(0, trim.length() - 1);
        }
        return trim;
    }

    private Double parseValue(String valueStr) {
        return Double.parseDouble(valueStr);
    }

    private void saveMeter(Metric metric) {
        this.result.add(metric);
        // cls很难计算count类型指标增量，内存计算增量
        Double delta = cache.getDelta(metric);
        if (delta != null) {
            Metric deltaMetric = new Gauge.Builder().setName(metric.getName() + "_delta").addTags(metric.getTags()).setValue(delta).build();;
            this.result.add(deltaMetric);
        }
    }

}

