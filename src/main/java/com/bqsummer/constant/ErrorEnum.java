package com.bqsummer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorEnum {

    PROMETHEUS_METRICS_PARSE_FAILED(10001, "prometheus response parse failed."),
    PROMETHEUS_METRIC_TYPE_ERROR(10002, "prometheus metric type error : %s");

    private int code;
    private String message;
}
