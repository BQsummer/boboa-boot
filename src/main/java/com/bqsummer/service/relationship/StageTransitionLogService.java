package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import com.bqsummer.common.dto.relationship.StageTransitionLog;
import com.bqsummer.common.vo.req.relationship.StageTransitionLogQueryRequest;
import com.bqsummer.common.vo.resp.relationship.StageTransitionLogResponse;
import com.bqsummer.mapper.StageTransitionLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StageTransitionLogService {

    private final StageTransitionLogMapper stageTransitionLogMapper;
    private final RelationshipStageService relationshipStageService;

    public IPage<StageTransitionLogResponse> list(StageTransitionLogQueryRequest request) {
        LambdaQueryWrapper<StageTransitionLog> wrapper = new LambdaQueryWrapper<>();
        if (request.getUserId() != null) {
            wrapper.eq(StageTransitionLog::getUserId, request.getUserId());
        }
        if (request.getAiCharacterId() != null) {
            wrapper.eq(StageTransitionLog::getAiCharacterId, request.getAiCharacterId());
        }
        if (request.getFromStageId() != null) {
            wrapper.eq(StageTransitionLog::getFromStageId, request.getFromStageId());
        }
        if (request.getToStageId() != null) {
            wrapper.eq(StageTransitionLog::getToStageId, request.getToStageId());
        }
        wrapper.orderByDesc(StageTransitionLog::getCreatedAt);

        Page<StageTransitionLog> page = new Page<>(request.getPage(), request.getPageSize());
        return stageTransitionLogMapper.selectPage(page, wrapper).convert(this::toResponse);
    }

    private StageTransitionLogResponse toResponse(StageTransitionLog log) {
        RelationshipStage fromStage = relationshipStageService.requireById(log.getFromStageId());
        RelationshipStage toStage = relationshipStageService.requireById(log.getToStageId());

        StageTransitionLogResponse response = new StageTransitionLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setAiCharacterId(log.getAiCharacterId());
        response.setFromStageId(log.getFromStageId());
        response.setFromStageCode(fromStage.getCode());
        response.setFromStageName(fromStage.getName());
        response.setToStageId(log.getToStageId());
        response.setToStageCode(toStage.getCode());
        response.setToStageName(toStage.getName());
        response.setReason(log.getReason());
        response.setDeltaScore(log.getDeltaScore());
        response.setMeta(log.getMeta());
        response.setCreatedAt(log.getCreatedAt());
        response.setUpdatedAt(log.getUpdatedAt());
        return response;
    }
}
