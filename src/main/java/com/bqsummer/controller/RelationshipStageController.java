package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.relationship.RelationshipStageCreateRequest;
import com.bqsummer.common.vo.req.relationship.RelationshipStageQueryRequest;
import com.bqsummer.common.vo.req.relationship.RelationshipStageUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.RelationshipStageResponse;
import com.bqsummer.service.relationship.RelationshipStageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relationship-stages")
@RequiredArgsConstructor
public class RelationshipStageController {

    private final RelationshipStageService relationshipStageService;

    @PostMapping
    public ResponseEntity<RelationshipStageResponse> create(@Valid @RequestBody RelationshipStageCreateRequest request) {
        return ResponseEntity.ok(relationshipStageService.create(request));
    }

    @GetMapping
    public ResponseEntity<IPage<RelationshipStageResponse>> list(@Valid RelationshipStageQueryRequest request) {
        return ResponseEntity.ok(relationshipStageService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RelationshipStageResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(relationshipStageService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RelationshipStageResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody RelationshipStageUpdateRequest request) {
        return ResponseEntity.ok(relationshipStageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        relationshipStageService.delete(id);
        return ResponseEntity.ok().build();
    }
}
