package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineCreateRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineQueryRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineUpdateRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessStepRequest;
import com.bqsummer.common.vo.resp.prompt.PostProcessPipelineResponse;
import com.bqsummer.common.vo.resp.prompt.PostProcessStepResponse;
import com.bqsummer.service.prompt.PostProcessPipelineService;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/post-process-pipelines")
@RequiredArgsConstructor
public class PostProcessPipelineController {

    private final PostProcessPipelineService postProcessPipelineService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<PostProcessPipelineResponse> create(
            @Valid @RequestBody PostProcessPipelineCreateRequest request,
            HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);
        return ResponseEntity.ok(postProcessPipelineService.create(request, currentUserId));
    }

    @GetMapping
    public ResponseEntity<IPage<PostProcessPipelineResponse>> list(@Valid PostProcessPipelineQueryRequest request) {
        return ResponseEntity.ok(postProcessPipelineService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostProcessPipelineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(postProcessPipelineService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostProcessPipelineResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PostProcessPipelineUpdateRequest request,
            HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);
        return ResponseEntity.ok(postProcessPipelineService.update(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);
        postProcessPipelineService.delete(id, currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<PostProcessStepResponse>> listSteps(@PathVariable Long id) {
        return ResponseEntity.ok(postProcessPipelineService.listSteps(id));
    }

    @PutMapping("/{id}/steps")
    public ResponseEntity<List<PostProcessStepResponse>> replaceSteps(
            @PathVariable Long id,
            @RequestBody List<@Valid PostProcessStepRequest> steps) {
        return ResponseEntity.ok(postProcessPipelineService.replaceSteps(id, steps));
    }
}
