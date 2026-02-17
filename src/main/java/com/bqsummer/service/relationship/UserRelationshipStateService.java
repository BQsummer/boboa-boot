package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import com.bqsummer.common.dto.relationship.StageTransitionLog;
import com.bqsummer.common.dto.relationship.StageTransitionRule;
import com.bqsummer.common.dto.relationship.UserRelationshipState;
import com.bqsummer.common.vo.req.relationship.RelationshipScoreAdjustRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateQueryRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateUpsertRequest;
import com.bqsummer.common.vo.resp.relationship.UserRelationshipStateResponse;
import com.bqsummer.mapper.StageTransitionLogMapper;
import com.bqsummer.mapper.StageTransitionRuleMapper;
import com.bqsummer.mapper.UserRelationshipStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRelationshipStateService {

    private final UserRelationshipStateMapper userRelationshipStateMapper;
    private final StageTransitionLogMapper stageTransitionLogMapper;
    private final StageTransitionRuleMapper stageTransitionRuleMapper;
    private final RelationshipStageService relationshipStageService;

    public IPage<UserRelationshipStateResponse> list(UserRelationshipStateQueryRequest request) {
        LambdaQueryWrapper<UserRelationshipState> wrapper = new LambdaQueryWrapper<>();
        if (request.getUserId() != null) {
            wrapper.eq(UserRelationshipState::getUserId, request.getUserId());
        }
        if (request.getAiCharacterId() != null) {
            wrapper.eq(UserRelationshipState::getAiCharacterId, request.getAiCharacterId());
        }
        if (request.getStageId() != null) {
            wrapper.eq(UserRelationshipState::getStageId, request.getStageId());
        }
        wrapper.orderByDesc(UserRelationshipState::getUpdatedAt);

        Page<UserRelationshipState> page = new Page<>(request.getPage(), request.getPageSize());
        return userRelationshipStateMapper.selectPage(page, wrapper).convert(this::toResponse);
    }

    @Transactional
    public UserRelationshipStateResponse upsert(UserRelationshipStateUpsertRequest request) {
        RelationshipStage targetStage = relationshipStageService.requireById(request.getStageId());

        UserRelationshipState current = userRelationshipStateMapper.findByUserAndCharacter(
                request.getUserId(), request.getAiCharacterId());

        LocalDateTime now = LocalDateTime.now();
        Integer fromStageId;
        Integer oldScore;

        if (current == null) {
            current = new UserRelationshipState();
            current.setUserId(request.getUserId());
            current.setAiCharacterId(request.getAiCharacterId());
            current.setStageId(request.getStageId());
            current.setStageScore(request.getStageScore());
            current.setCreatedAt(now);
            current.setUpdatedAt(now);
            userRelationshipStateMapper.insert(current);

            fromStageId = request.getStageId();
            oldScore = 0;
        } else {
            fromStageId = current.getStageId();
            oldScore = current.getStageScore() == null ? 0 : current.getStageScore();
            current.setStageId(request.getStageId());
            current.setStageScore(request.getStageScore());
            current.setUpdatedAt(now);
            userRelationshipStateMapper.updateById(current);
        }

        int delta = request.getStageScore() - oldScore;
        insertTransitionLog(request.getUserId(), request.getAiCharacterId(), fromStageId, targetStage.getId(),
                request.getReason(), delta, request.getMeta(), now);

        return toResponse(current);
    }

    @Transactional
    public UserRelationshipStateResponse increaseScore(RelationshipScoreAdjustRequest request) {
        return adjustScore(request, true);
    }

    @Transactional
    public UserRelationshipStateResponse decreaseScore(RelationshipScoreAdjustRequest request) {
        return adjustScore(request, false);
    }

    private UserRelationshipStateResponse adjustScore(RelationshipScoreAdjustRequest request, boolean increase) {
        int delta = request.getDelta() == null ? 0 : request.getDelta();
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be > 0");
        }
        delta = increase ? delta : -delta;

        UserRelationshipState state = getOrInitState(request.getUserId(), request.getAiCharacterId());
        LocalDateTime now = LocalDateTime.now();

        int oldScore = state.getStageScore() == null ? 0 : state.getStageScore();
        int newScore = Math.max(0, oldScore + delta);

        Integer fromStageId = state.getStageId();
        Integer toStageId = resolveStageAfterScoreChange(
                request.getUserId(), request.getAiCharacterId(), fromStageId, newScore, delta > 0, now);

        state.setStageScore(newScore);
        state.setStageId(toStageId);
        state.setUpdatedAt(now);
        userRelationshipStateMapper.updateById(state);

        insertTransitionLog(
                request.getUserId(),
                request.getAiCharacterId(),
                fromStageId,
                toStageId,
                request.getReason(),
                delta,
                request.getMeta(),
                now
        );

        return toResponse(state);
    }

    private Integer resolveStageAfterScoreChange(Long userId,
                                                  Long aiCharacterId,
                                                  Integer currentStageId,
                                                  int newScore,
                                                  boolean increase,
                                                  LocalDateTime now) {
        String direction = increase ? "up" : "down";
        List<StageTransitionRule> rules = stageTransitionRuleMapper.findActiveRulesByFromAndDirection(currentStageId, direction);
        if (rules == null || rules.isEmpty()) {
            return currentStageId;
        }

        List<StageTransitionRule> matched = new ArrayList<>();
        for (StageTransitionRule rule : rules) {
            if (!isThresholdMatched(rule, newScore, increase)) {
                continue;
            }
            if (!isCooldownReady(userId, aiCharacterId, rule, now)) {
                continue;
            }
            matched.add(rule);
        }

        if (matched.isEmpty()) {
            return currentStageId;
        }

        matched.sort((a, b) -> {
            RelationshipStage stageA = relationshipStageService.requireById(a.getToStageId());
            RelationshipStage stageB = relationshipStageService.requireById(b.getToStageId());
            return increase
                    ? Comparator.comparing(RelationshipStage::getLevel).reversed().compare(stageA, stageB)
                    : Comparator.comparing(RelationshipStage::getLevel).compare(stageA, stageB);
        });

        return matched.get(0).getToStageId();
    }

    private boolean isThresholdMatched(StageTransitionRule rule, int score, boolean increase) {
        if (increase) {
            return rule.getMinScore() != null && score >= rule.getMinScore();
        }
        return rule.getMaxScore() != null && score <= rule.getMaxScore();
    }

    private boolean isCooldownReady(Long userId,
                                    Long aiCharacterId,
                                    StageTransitionRule rule,
                                    LocalDateTime now) {
        if (rule.getCooldownSec() == null || rule.getCooldownSec() <= 0) {
            return true;
        }

        LambdaQueryWrapper<StageTransitionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StageTransitionLog::getUserId, userId)
                .eq(StageTransitionLog::getAiCharacterId, aiCharacterId)
                .eq(StageTransitionLog::getFromStageId, rule.getFromStageId())
                .eq(StageTransitionLog::getToStageId, rule.getToStageId())
                .orderByDesc(StageTransitionLog::getCreatedAt)
                .last("LIMIT 1");

        List<StageTransitionLog> logs = stageTransitionLogMapper.selectList(wrapper);
        if (logs == null || logs.isEmpty() || logs.get(0).getCreatedAt() == null) {
            return true;
        }

        long seconds = Duration.between(logs.get(0).getCreatedAt(), now).getSeconds();
        return seconds >= rule.getCooldownSec();
    }

    private UserRelationshipState getOrInitState(Long userId, Long aiCharacterId) {
        UserRelationshipState state = userRelationshipStateMapper.findByUserAndCharacter(userId, aiCharacterId);
        if (state != null) {
            return state;
        }

        RelationshipStage initialStage = relationshipStageService.requireFirstActiveStage();
        LocalDateTime now = LocalDateTime.now();

        state = new UserRelationshipState();
        state.setUserId(userId);
        state.setAiCharacterId(aiCharacterId);
        state.setStageId(initialStage.getId());
        state.setStageScore(0);
        state.setCreatedAt(now);
        state.setUpdatedAt(now);
        userRelationshipStateMapper.insert(state);
        return state;
    }

    private void insertTransitionLog(Long userId,
                                     Long aiCharacterId,
                                     Integer fromStageId,
                                     Integer toStageId,
                                     String reason,
                                     Integer delta,
                                     Map<String, Object> meta,
                                     LocalDateTime now) {
        StageTransitionLog log = new StageTransitionLog();
        log.setUserId(userId);
        log.setAiCharacterId(aiCharacterId);
        log.setFromStageId(fromStageId);
        log.setToStageId(toStageId);
        log.setReason(reason);
        log.setDeltaScore(delta == null ? 0 : delta);
        log.setMeta(meta);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        stageTransitionLogMapper.insert(log);
    }

    private UserRelationshipStateResponse toResponse(UserRelationshipState state) {
        RelationshipStage stage = relationshipStageService.requireById(state.getStageId());

        UserRelationshipStateResponse response = new UserRelationshipStateResponse();
        response.setId(state.getId());
        response.setUserId(state.getUserId());
        response.setAiCharacterId(state.getAiCharacterId());
        response.setStageId(state.getStageId());
        response.setStageCode(stage.getCode());
        response.setStageName(stage.getName());
        response.setStageLevel(stage.getLevel());
        response.setStageScore(state.getStageScore());
        response.setCreatedAt(state.getCreatedAt());
        response.setUpdatedAt(state.getUpdatedAt());
        return response;
    }
}
