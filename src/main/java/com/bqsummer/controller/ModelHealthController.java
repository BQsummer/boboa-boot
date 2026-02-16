package com.bqsummer.controller;

import com.bqsummer.common.dto.ai.HealthStatus;
import com.bqsummer.common.dto.ai.ModelHealthStatus;
import com.bqsummer.service.ai.ModelHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model health check controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class ModelHealthController {

    private static final String MSG_OK = "\u67e5\u8be2\u6210\u529f";
    private static final String MSG_CHECK_TRIGGERED = "\u5065\u5eb7\u68c0\u67e5\u5df2\u89e6\u53d1";
    private static final String MSG_BATCH_TRIGGERED = "\u6279\u91cf\u5065\u5eb7\u68c0\u67e5\u5df2\u89e6\u53d1";

    private final ModelHealthService healthService;

    /**
     * Get all model health status records.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllHealthStatus() {
        List<ModelHealthStatus> statusList = healthService.getAllHealthStatus();
        if (statusList == null) {
            statusList = Collections.emptyList();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", MSG_OK);
        result.put("data", statusList);

        return ResponseEntity.ok(result);
    }

    /**
     * Get health status by model id.
     */
    @GetMapping("/{modelId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getHealthStatus(@PathVariable Long modelId) {
        ModelHealthStatus status = healthService.getHealthStatus(modelId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", MSG_OK);
        result.put("data", status);

        return ResponseEntity.ok(result);
    }

    /**
     * Get summary stats for dashboard cards.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        List<ModelHealthStatus> statusList = healthService.getAllHealthStatus();
        if (statusList == null) {
            statusList = Collections.emptyList();
        }

        int total = statusList.size();
        int online = 0;
        int offline = 0;
        int timeout = 0;
        int authFailed = 0;
        BigDecimal uptimeSum = BigDecimal.ZERO;
        int uptimeCount = 0;

        for (ModelHealthStatus item : statusList) {
            HealthStatus status = item.getStatus();
            if (status == HealthStatus.ONLINE) {
                online++;
            } else if (status == HealthStatus.OFFLINE) {
                offline++;
            } else if (status == HealthStatus.TIMEOUT) {
                timeout++;
            } else if (status == HealthStatus.AUTH_FAILED) {
                authFailed++;
            }

            if (item.getUptimePercentage() != null) {
                uptimeSum = uptimeSum.add(item.getUptimePercentage());
                uptimeCount++;
            }
        }

        BigDecimal avgUptime = null;
        if (uptimeCount > 0) {
            avgUptime = uptimeSum.divide(BigDecimal.valueOf(uptimeCount), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("online", online);
        summary.put("offline", offline);
        summary.put("timeout", timeout);
        summary.put("authFailed", authFailed);
        summary.put("avgUptime", avgUptime);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", MSG_OK);
        result.put("data", summary);

        return ResponseEntity.ok(result);
    }

    /**
     * Trigger health check for one model.
     */
    @PostMapping("/{modelId}/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performHealthCheck(@PathVariable Long modelId) {
        log.info("Trigger model health check: modelId={}", modelId);

        healthService.performHealthCheck(modelId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", MSG_CHECK_TRIGGERED);

        return ResponseEntity.ok(result);
    }

    /**
     * Trigger batch health checks.
     */
    @PostMapping("/batch-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> performBatchHealthCheck() {
        log.info("Trigger batch model health checks");

        healthService.performBatchHealthCheck();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", MSG_BATCH_TRIGGERED);

        return ResponseEntity.ok(result);
    }
}
