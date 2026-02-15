package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.recharge.RechargeOrder;
import com.bqsummer.service.recharge.RechargeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/recharge")
@RequiredArgsConstructor
public class RechargeAdminController {

    private final RechargeAdminService rechargeAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RechargeOrder>> list(@RequestParam(required = false) String orderNo,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String channel,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdStart,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdEnd,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size) {
        return ResponseEntity.ok(rechargeAdminService.list(
                orderNo,
                userId,
                status,
                channel,
                createdStart,
                createdEnd,
                page,
                size
        ));
    }

    @GetMapping("/{orderNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RechargeOrder> detail(@PathVariable String orderNo) {
        return ResponseEntity.ok(rechargeAdminService.detail(orderNo));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(rechargeAdminService.stats());
    }

    @PostMapping("/{orderNo}/success")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markSuccess(@PathVariable String orderNo) {
        rechargeAdminService.markSuccess(orderNo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderNo}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> closePending(@PathVariable String orderNo) {
        rechargeAdminService.closePending(orderNo);
        return ResponseEntity.ok().build();
    }
}
