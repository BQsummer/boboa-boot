package com.bqsummer.controller;

import com.bqsummer.common.vo.req.chararcter.CreateMonthlyPlanReq;
import com.bqsummer.common.vo.req.chararcter.UpdateMonthlyPlanReq;
import com.bqsummer.service.MonthlyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 月度计划控制器
 */
@RestController
@RequestMapping("/api/v1/ai/characters")
@RequiredArgsConstructor
public class MonthlyPlanController {

    private final MonthlyPlanService monthlyPlanService;

    /**
     * 创建月度计划
     */
    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@RequestBody CreateMonthlyPlanReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return monthlyPlanService.createPlan(req, userId);
    }

    /**
     * 获取虚拟人物的所有计划
     */
    @GetMapping("/{characterId}/plans")
    public ResponseEntity<?> listPlansByCharacter(@PathVariable("characterId") Long characterId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return monthlyPlanService.listPlansByCharacterId(characterId, userId);
    }

    /**
     * 获取计划详情
     */
    @GetMapping("/plans/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return monthlyPlanService.getPlanById(id, userId);
    }

    /**
     * 更新月度计划
     */
    @PutMapping("/plans/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable("id") Long id, @RequestBody UpdateMonthlyPlanReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return monthlyPlanService.updatePlan(id, req, userId);
    }

    /**
     * 删除月度计划（软删除）
     */
    @DeleteMapping("/plans/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return monthlyPlanService.deletePlan(id, userId);
    }
}
