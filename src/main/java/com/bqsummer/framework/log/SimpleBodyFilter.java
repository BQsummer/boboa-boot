package com.bqsummer.framework.log;

import org.zalando.logbook.BodyFilter;

public class SimpleBodyFilter implements BodyFilter {
    @Override
    public String filter(String contentType, String body) {
        if (body == null || body.isEmpty()) return body;
        String masked = body;
        // JSON: "password":"..."
        masked = masked.replaceAll("(?i)\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\"password\":\"***\"");
        // form/text: password=...
        masked = masked.replaceAll("(?i)(password=)([^&\\s]*)", "$1***");
        return masked;
    }
}
