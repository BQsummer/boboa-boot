package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.relationship.RelationshipScoreAdjustRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateQueryRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateUpsertRequest;
import com.bqsummer.common.vo.resp.relationship.UserRelationshipStateResponse;
import com.bqsummer.service.relationship.UserRelationshipStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-relationship-states")
@RequiredArgsConstructor
public class UserRelationshipStateController {

    private final UserRelationshipStateService userRelationshipStateService;

    @GetMapping
    public ResponseEntity<IPage<UserRelationshipStateResponse>> list(@Valid UserRelationshipStateQueryRequest request) {
        return ResponseEntity.ok(userRelationshipStateService.list(request));
    }

    @PostMapping("/upsert")
    public ResponseEntity<UserRelationshipStateResponse> upsert(
            @Valid @RequestBody UserRelationshipStateUpsertRequest request) {
        return ResponseEntity.ok(userRelationshipStateService.upsert(request));
    }

    @PostMapping("/increase-score")
    public ResponseEntity<UserRelationshipStateResponse> increaseScore(
            @Valid @RequestBody RelationshipScoreAdjustRequest request) {
        return ResponseEntity.ok(userRelationshipStateService.increaseScore(request));
    }

    @PostMapping("/decrease-score")
    public ResponseEntity<UserRelationshipStateResponse> decreaseScore(
            @Valid @RequestBody RelationshipScoreAdjustRequest request) {
        return ResponseEntity.ok(userRelationshipStateService.decreaseScore(request));
    }
}
