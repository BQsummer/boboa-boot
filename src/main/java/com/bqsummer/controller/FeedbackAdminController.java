package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.feedback.Feedback;
import com.bqsummer.common.vo.req.feedback.BatchUpdateStatusRequest;
import com.bqsummer.common.vo.req.feedback.UpdateFeedbackStatusRequest;
import com.bqsummer.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/feedback")
@RequiredArgsConstructor
public class FeedbackAdminController {

    private final FeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Feedback>> list(@RequestParam(required = false) String type,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(feedbackService.searchList(type, status, userId, keyword, page, size));
        }
        return ResponseEntity.ok(feedbackService.list(type, status, userId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Feedback> detail(@PathVariable Long id) {
        Feedback fb = feedbackService.detail(id);
        if (fb == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fb);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateFeedbackStatusRequest req) {
        int rows = feedbackService.updateStatus(id, req.getStatus(), req.getRemark(), (Long) SecurityContextHolder.getContext().getAuthentication().getDetails());
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        int rows = feedbackService.delete(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(feedbackService.getStats());
    }

    @PutMapping("/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchUpdateStatus(@Valid @RequestBody BatchUpdateStatusRequest req) {
        Long handlerUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        int updatedCount = feedbackService.batchUpdateStatus(req.getIds(), req.getStatus(), req.getRemark(), handlerUserId);
        Map<String, Object> result = Map.of("updatedCount", updatedCount);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        int deletedCount = feedbackService.batchDelete(ids);
        Map<String, Object> result = Map.of("deletedCount", deletedCount);
        return ResponseEntity.ok(result);
    }
}
