package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.relationship.StagePromptCreateRequest;
import com.bqsummer.common.vo.req.relationship.StagePromptQueryRequest;
import com.bqsummer.common.vo.req.relationship.StagePromptUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.StagePromptResponse;
import com.bqsummer.service.relationship.StagePromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stage-prompts")
@RequiredArgsConstructor
public class StagePromptController {

    private final StagePromptService stagePromptService;

    @PostMapping
    public ResponseEntity<StagePromptResponse> create(@Valid @RequestBody StagePromptCreateRequest request) {
        return ResponseEntity.ok(stagePromptService.create(request));
    }

    @GetMapping
    public ResponseEntity<IPage<StagePromptResponse>> list(@Valid StagePromptQueryRequest request) {
        return ResponseEntity.ok(stagePromptService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StagePromptResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stagePromptService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StagePromptResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StagePromptUpdateRequest request) {
        return ResponseEntity.ok(stagePromptService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stagePromptService.delete(id);
        return ResponseEntity.ok().build();
    }
}
