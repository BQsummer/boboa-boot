package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.TaskStatus;
import com.bqsummer.mapper.RobotTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
public class RobotTaskAdminController {

    private final RobotTaskMapper robotTaskMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RobotTask>> list(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) String actionType,
                                                @RequestParam(required = false) String taskType,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Long robotId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledStart,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledEnd,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size) {
        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        if (hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (hasText(actionType)) {
            wrapper.eq("action_type", actionType.trim());
        }
        if (hasText(taskType)) {
            wrapper.eq("task_type", taskType.trim());
        }
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (robotId != null) {
            wrapper.eq("robot_id", robotId);
        }
        if (scheduledStart != null) {
            wrapper.ge("scheduled_at", scheduledStart);
        }
        if (scheduledEnd != null) {
            wrapper.le("scheduled_at", scheduledEnd);
        }
        if (hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like("action_payload", value)
                    .or()
                    .like("error_message", value)
                    .or()
                    .like("locked_by", value));
        }

        wrapper.orderByDesc("created_time");
        Page<RobotTask> result = robotTaskMapper.selectPage(new Page<>(page, size), wrapper);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RobotTask> detail(@PathVariable Long id) {
        RobotTask task = robotTaskMapper.selectById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();

        long totalCount = countBy(new QueryWrapper<>());
        long pendingCount = countByStatus(TaskStatus.PENDING.name());
        long runningCount = countByStatus(TaskStatus.RUNNING.name());
        long doneCount = countByStatus(TaskStatus.DONE.name());
        long failedCount = countByStatus(TaskStatus.FAILED.name());
        long timeoutCount = countByStatus(TaskStatus.TIMEOUT.name());

        QueryWrapper<RobotTask> overduePendingWrapper = new QueryWrapper<>();
        overduePendingWrapper.eq("status", TaskStatus.PENDING.name())
                .le("scheduled_at", LocalDateTime.now());
        long overduePendingCount = countBy(overduePendingWrapper);

        QueryWrapper<RobotTask> todayCreatedWrapper = new QueryWrapper<>();
        todayCreatedWrapper.ge("created_time", LocalDate.now().atStartOfDay());
        long todayCreatedCount = countBy(todayCreatedWrapper);

        List<Map<String, Object>> actionTypeStats = robotTaskMapper.selectMaps(
                new QueryWrapper<RobotTask>()
                        .select("action_type", "count(1) as cnt")
                        .groupBy("action_type")
        );

        result.put("totalCount", totalCount);
        result.put("pendingCount", pendingCount);
        result.put("runningCount", runningCount);
        result.put("doneCount", doneCount);
        result.put("failedCount", failedCount);
        result.put("timeoutCount", timeoutCount);
        result.put("overduePendingCount", overduePendingCount);
        result.put("todayCreatedCount", todayCreatedCount);
        result.put("actionTypeStats", actionTypeStats);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retry(@PathVariable Long id) {
        RobotTask task = robotTaskMapper.selectById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        String status = task.getStatus();
        if (!TaskStatus.FAILED.name().equals(status) && !TaskStatus.TIMEOUT.name().equals(status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "仅 FAILED/TIMEOUT 任务可重试",
                    "taskId", id,
                    "status", status
            ));
        }

        UpdateWrapper<RobotTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .eq("status", status)
                .set("status", TaskStatus.PENDING.name())
                .set("locked_by", null)
                .set("started_at", null)
                .set("completed_at", null)
                .set("heartbeat_at", null)
                .set("error_message", null)
                .set("retry_count", 0)
                .set("scheduled_at", LocalDateTime.now())
                .set("updated_time", LocalDateTime.now());

        int updated = robotTaskMapper.update(null, wrapper);
        if (updated == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "任务状态已变化，请刷新后重试",
                    "taskId", id
            ));
        }
        return ResponseEntity.ok(Map.of(
                "message", "任务已重置为待执行",
                "taskId", id
        ));
    }

    private long countBy(QueryWrapper<RobotTask> wrapper) {
        Long value = robotTaskMapper.selectCount(wrapper);
        return value == null ? 0L : value;
    }

    private long countByStatus(String status) {
        QueryWrapper<RobotTask> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        return countBy(wrapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
