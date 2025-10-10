package com.bqsummer.framework.log;

import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.HttpHeaders;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleHeaderFilter implements HeaderFilter {
    private static final Set<String> ALLOWED_LOWER = Set.of("x-forwarded-for", "user-agent", "content-type");

    @Override
    public HttpHeaders filter(HttpHeaders headers) {
        Map<String, List<String>> filtered = headers.entrySet().stream()
                .filter(e -> ALLOWED_LOWER.contains(e.getKey().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return HttpHeaders.of(filtered);
    }
}
