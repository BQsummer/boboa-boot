package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import com.bqsummer.common.dto.relationship.StageTransitionRule;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleCreateRequest;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleQueryRequest;
import com.bqsummer.common.vo.req.relationship.StageTransitionRuleUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.StageTransitionRuleResponse;
import com.bqsummer.mapper.StageTransitionRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StageTransitionRuleService {

    private static final Set<String> ALLOWED_DIRECTION = Set.of("up", "down");

    private final StageTransitionRuleMapper stageTransitionRuleMapper;
    private final RelationshipStageService relationshipStageService;

    @Transactional
    public StageTransitionRuleResponse create(StageTransitionRuleCreateRequest request) {
        StageTransitionRule rule = new StageTransitionRule();
        rule.setFromStageId(request.getFromStageId());
        rule.setToStageId(request.getToStageId());
        rule.setDirection(normalizeDirection(request.getDirection()));
        rule.setMinScore(request.getMinScore());
        rule.setMaxScore(request.getMaxScore());
        rule.setCooldownSec(request.getCooldownSec());
        rule.setCondition(request.getCondition());
        rule.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());

        validateRule(rule);
        stageTransitionRuleMapper.insert(rule);
        return toResponse(rule);
    }

    public IPage<StageTransitionRuleResponse> list(StageTransitionRuleQueryRequest request) {
        LambdaQueryWrapper<StageTransitionRule> wrapper = new LambdaQueryWrapper<>();
        if (request.getFromStageId() != null) {
            wrapper.eq(StageTransitionRule::getFromStageId, request.getFromStageId());
        }
        if (request.getToStageId() != null) {
            wrapper.eq(StageTransitionRule::getToStageId, request.getToStageId());
        }
        if (StringUtils.hasText(request.getDirection())) {
            wrapper.eq(StageTransitionRule::getDirection, normalizeDirection(request.getDirection()));
        }
        if (request.getIsActive() != null) {
            wrapper.eq(StageTransitionRule::getIsActive, request.getIsActive());
        }
        wrapper.orderByDesc(StageTransitionRule::getCreatedAt);

        Page<StageTransitionRule> page = new Page<>(request.getPage(), request.getPageSize());
        return stageTransitionRuleMapper.selectPage(page, wrapper).convert(this::toResponse);
    }

    public StageTransitionRuleResponse getById(Long id) {
        return toResponse(requireRule(id));
    }

    @Transactional
    public StageTransitionRuleResponse update(Long id, StageTransitionRuleUpdateRequest request) {
        StageTransitionRule rule = requireRule(id);

        if (request.getFromStageId() != null) {
            rule.setFromStageId(request.getFromStageId());
        }
        if (request.getToStageId() != null) {
            rule.setToStageId(request.getToStageId());
        }
        if (request.getDirection() != null) {
            rule.setDirection(normalizeDirection(request.getDirection()));
        }
        if (request.getMinScore() != null) {
            rule.setMinScore(request.getMinScore());
        }
        if (request.getMaxScore() != null) {
            rule.setMaxScore(request.getMaxScore());
        }
        if (request.getCooldownSec() != null) {
            rule.setCooldownSec(request.getCooldownSec());
        }
        if (request.getCondition() != null) {
            rule.setCondition(request.getCondition());
        }
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        rule.setUpdatedAt(LocalDateTime.now());
        validateRule(rule);
        stageTransitionRuleMapper.updateById(rule);
        return toResponse(rule);
    }

    @Transactional
    public void delete(Long id) {
        requireRule(id);
        stageTransitionRuleMapper.deleteById(id);
    }

    private StageTransitionRule requireRule(Long id) {
        StageTransitionRule rule = stageTransitionRuleMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("stage transition rule not found, id=" + id);
        }
        return rule;
    }

    private void validateRule(StageTransitionRule rule) {
        relationshipStageService.requireById(rule.getFromStageId());
        relationshipStageService.requireById(rule.getToStageId());
        if (rule.getFromStageId().equals(rule.getToStageId())) {
            throw new IllegalArgumentException("fromStageId and toStageId must be different");
        }
        if (!ALLOWED_DIRECTION.contains(rule.getDirection())) {
            throw new IllegalArgumentException("invalid direction: " + rule.getDirection());
        }
        if ("up".equals(rule.getDirection()) && rule.getMinScore() == null) {
            throw new IllegalArgumentException("minScore is required for up direction");
        }
        if ("down".equals(rule.getDirection()) && rule.getMaxScore() == null) {
            throw new IllegalArgumentException("maxScore is required for down direction");
        }
        if (rule.getCooldownSec() == null || rule.getCooldownSec() < 0) {
            throw new IllegalArgumentException("cooldownSec must be >= 0");
        }
    }

    private String normalizeDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            throw new IllegalArgumentException("direction is required");
        }
        String normalized = direction.trim().toLowerCase();
        if (!ALLOWED_DIRECTION.contains(normalized)) {
            throw new IllegalArgumentException("invalid direction: " + direction);
        }
        return normalized;
    }

    private StageTransitionRuleResponse toResponse(StageTransitionRule rule) {
        RelationshipStage fromStage = relationshipStageService.requireById(rule.getFromStageId());
        RelationshipStage toStage = relationshipStageService.requireById(rule.getToStageId());

        StageTransitionRuleResponse response = new StageTransitionRuleResponse();
        response.setId(rule.getId());
        response.setFromStageId(rule.getFromStageId());
        response.setFromStageCode(fromStage.getCode());
        response.setFromStageName(fromStage.getName());
        response.setToStageId(rule.getToStageId());
        response.setToStageCode(toStage.getCode());
        response.setToStageName(toStage.getName());
        response.setDirection(rule.getDirection());
        response.setMinScore(rule.getMinScore());
        response.setMaxScore(rule.getMaxScore());
        response.setCooldownSec(rule.getCooldownSec());
        response.setCondition(rule.getCondition());
        response.setIsActive(rule.getIsActive());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}
