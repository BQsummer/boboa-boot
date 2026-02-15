package com.bqsummer.framework.http;

import com.bqsummer.configuration.Configs;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Per-IP rate limiting filter based on Guava RateLimiter with optional blacklist.
 */
@Order(1)
@Slf4j
@Component
public class IpRateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private Configs configs;

    private final Cache<String, RateLimiter> perIpLimiter;
    private final double permitsPerSecond;
    private final int tryAcquireTimeoutMillis;

    @Autowired
    public IpRateLimitFilter(
            @Value("${ip.rate.limit.permits-per-second:10}") double permitsPerSecond,
            @Value("${ip.rate.limit.timeout-millis:0}") int tryAcquireTimeoutMillis,
            @Value("${ip.rate.limit.cache-expire-minutes:30}") long expireMinutes,
            @Value("${ip.rate.limit.cache-max-size:10000}") long maxSize
    ) {
        this.permitsPerSecond = Math.max(permitsPerSecond, 0.0001d);
        this.tryAcquireTimeoutMillis = Math.max(tryAcquireTimeoutMillis, 0);
        this.perIpLimiter = Caffeine.newBuilder()
                .maximumSize(Math.max(maxSize, 100))
                .expireAfterAccess(Math.max(expireMinutes, 1), TimeUnit.MINUTES)
                .build();
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        Set<String> ipBlackSet = parseIpSet(configs.getIpBlackList());

        // Blacklist check first
        if (isBlacklisted(clientIp, ipBlackSet)) {
            respondForbidden(response, clientIp);
            return;
        }
        String blacklistedParamIp = findBlacklistedRequestParamIp(request, ipBlackSet);
        if (blacklistedParamIp != null) {
            respondForbidden(response, blacklistedParamIp);
            return;
        }
        if(StringUtils.isNotBlank(configs.getIpWhiteList())) {
            Set<String> ipWhiteSet = parseIpSet(configs.getIpWhiteList());
            boolean isWhite = ipWhiteSet.contains(clientIp);
            if(!isWhite) {
                RateLimiter limiter = perIpLimiter.get(clientIp, ip -> RateLimiter.create(permitsPerSecond));

                boolean allowed;
                if (tryAcquireTimeoutMillis > 0) {
                    allowed = limiter.tryAcquire(1, tryAcquireTimeoutMillis, TimeUnit.MILLISECONDS);
                } else {
                    allowed = limiter.tryAcquire();
                }

                if (!allowed) {
                    // Too many requests
                    log.error("IP {} is rate limited", clientIp);
                    respondTooManyRequests(response, clientIp);
                    return;
                }
            }
        }



        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String ip, Set<String> ipBlackSet) {
        if (StringUtils.isBlank(ip) || ipBlackSet.isEmpty()) {
            return false;
        }
        return ipBlackSet.contains(ip);
    }

    private String findBlacklistedRequestParamIp(HttpServletRequest request, Set<String> ipBlackSet) {
        if (request == null || ipBlackSet.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                continue;
            }
            for (String value : values) {
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                String trimmedValue = value.trim();
                if (ipBlackSet.contains(trimmedValue)) {
                    return trimmedValue;
                }
            }
        }
        return null;
    }

    private Set<String> parseIpSet(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptySet();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void respondTooManyRequests(HttpServletResponse response, String ip) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":430,\"message\":\"Too many requests from IP: " + escapeJson(ip) + "\"}";
        response.getWriter().write(body);
    }

    private void respondForbidden(HttpServletResponse response, String ip) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":403,\"message\":\"IP is blacklisted: " + escapeJson(ip) + "\"}";
        response.getWriter().write(body);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Order: X-Forwarded-For -> X-Real-IP -> remoteAddr
        String xff = headerFirstIp(request.getHeader("X-Forwarded-For"));
        if (notBlank(xff)) return xff;
        String xri = request.getHeader("X-Real-IP");
        if (notBlank(xri)) return xri.trim();
        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }

    private String headerFirstIp(String headerValue) {
        if (!notBlank(headerValue)) return null;
        String[] parts = headerValue.split(",");
        if (parts.length == 0) return null;
        return parts[0].trim();
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty() && !"unknown".equalsIgnoreCase(s.trim());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
