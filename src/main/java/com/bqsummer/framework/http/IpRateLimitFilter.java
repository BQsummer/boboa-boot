package com.bqsummer.framework.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Per-IP rate limiting filter based on Guava RateLimiter with optional blacklist.
 */
@Slf4j
@Component
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, RateLimiter> perIpLimiter;
    private final double permitsPerSecond;
    private final int tryAcquireTimeoutMillis;
    private final Set<String> ipBlacklist;

    @Autowired
    public IpRateLimitFilter(
            @Value("${ip.rate.limit.permits-per-second:10}") double permitsPerSecond,
            @Value("${ip.rate.limit.timeout-millis:0}") int tryAcquireTimeoutMillis,
            @Value("${ip.rate.limit.cache-expire-minutes:30}") long expireMinutes,
            @Value("${ip.rate.limit.cache-max-size:10000}") long maxSize,
            @Value("${ip.rate.limit.blacklist:}") String blacklistCsv
    ) {
        this.permitsPerSecond = Math.max(permitsPerSecond, 0.0001d);
        this.tryAcquireTimeoutMillis = Math.max(tryAcquireTimeoutMillis, 0);
        this.perIpLimiter = Caffeine.newBuilder()
                .maximumSize(Math.max(maxSize, 100))
                .expireAfterAccess(Math.max(expireMinutes, 1), TimeUnit.MINUTES)
                .build();
        this.ipBlacklist = parseBlacklist(blacklistCsv);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);

        // Blacklist check first
        if (isBlacklisted(clientIp)) {
            respondForbidden(response, clientIp);
            return;
        }

        RateLimiter limiter = perIpLimiter.get(clientIp, ip -> RateLimiter.create(permitsPerSecond));

        boolean allowed;
        if (tryAcquireTimeoutMillis > 0) {
            allowed = limiter.tryAcquire(1, tryAcquireTimeoutMillis, TimeUnit.MILLISECONDS);
        } else {
            allowed = limiter.tryAcquire();
        }

        if (!allowed) {
            // Too many requests
            respondTooManyRequests(response, clientIp);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String ip) {
        return ipBlacklist.contains(ip);
    }

    private Set<String> parseBlacklist(String csv) {
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
        String body = "{\"code\":429,\"message\":\"Too many requests from IP: " + escapeJson(ip) + "\"}";
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
