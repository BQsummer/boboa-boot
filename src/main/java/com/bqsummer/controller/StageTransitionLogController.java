package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.relationship.StageTransitionLogQueryRequest;
import com.bqsummer.common.vo.resp.relationship.StageTransitionLogResponse;
import com.bqsummer.service.relationship.StageTransitionLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stage-transition-logs")
@RequiredArgsConstructor
public class StageTransitionLogController {

    private final StageTransitionLogService stageTransitionLogService;

    @GetMapping
    public ResponseEntity<IPage<StageTransitionLogResponse>> list(@Valid StageTransitionLogQueryRequest request) {
        return ResponseEntity.ok(stageTransitionLogService.list(request));
    }
}
