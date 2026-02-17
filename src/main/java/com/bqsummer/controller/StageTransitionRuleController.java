package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleCreateRequest;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleQueryRequest;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.StageTransitionRuleResponse;
import com.bqsummer.service.relationship.StageTransitionRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stage-transition-rules")
@RequiredArgsConstructor
public class StageTransitionRuleController {

    private final StageTransitionRuleService stageTransitionRuleService;

    @PostMapping
    public ResponseEntity<StageTransitionRuleResponse> create(
            @Valid @RequestBody StageTransitionRuleCreateRequest request) {
        return ResponseEntity.ok(stageTransitionRuleService.create(request));
    }

    @GetMapping
    public ResponseEntity<IPage<StageTransitionRuleResponse>> list(@Valid StageTransitionRuleQueryRequest request) {
        return ResponseEntity.ok(stageTransitionRuleService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StageTransitionRuleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stageTransitionRuleService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StageTransitionRuleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StageTransitionRuleUpdateRequest request) {
        return ResponseEntity.ok(stageTransitionRuleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageTransitionRuleService.delete(id);
        return ResponseEntity.ok().build();
    }
}
