package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.plugin.points.domain.PointsActivity;
import com.bqsummer.plugin.points.domain.PointsAccount;
import com.bqsummer.plugin.points.domain.PointsTransaction;
import com.bqsummer.plugin.points.service.PointsService;
import com.bqsummer.plugin.points.service.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @PostMapping("/earn")
    public ResponseEntity<Void> earn(@Valid @RequestBody EarnPointsRequest request) {
        pointsService.earn(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@Valid @RequestBody ConsumePointsRequest request) {
        pointsService.consume(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/balance")
    public ResponseEntity<PointsAccount> balance(@RequestParam Long userId) {
        return ResponseEntity.ok(pointsService.getAccount(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<PointsTransaction>> transactions(@RequestParam(required = false) Long userId,
                                                                @RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size) {
        return ResponseEntity.ok(pointsService.listTransactions(userId, page, size));
    }

    @PostMapping("/activities")
    public ResponseEntity<Void> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        pointsService.createActivity(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/activities/{code}")
    public ResponseEntity<Void> updateActivity(@PathVariable String code, @RequestBody UpdateActivityRequest request) {
        pointsService.updateActivity(code, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activities")
    public ResponseEntity<List<PointsActivity>> listActivities() {
        return ResponseEntity.ok(pointsService.listActivities());
    }
}

