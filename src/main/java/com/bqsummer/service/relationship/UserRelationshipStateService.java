package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.InteractionLog;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import com.bqsummer.common.dto.relationship.StageTransitionLog;
import com.bqsummer.common.dto.relationship.StageTransitionRule;
import com.bqsummer.common.dto.relationship.UserRelationshipState;
import com.bqsummer.common.vo.req.relationship.RelationshipInteractionSignalRequest;
import com.bqsummer.common.vo.req.relationship.RelationshipScoreAdjustRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateQueryRequest;
import com.bqsummer.common.vo.req.relationship.UserRelationshipStateUpsertRequest;
import com.bqsummer.common.vo.resp.relationship.UserRelationshipStateResponse;
import com.bqsummer.configuration.Configs;
import com.bqsummer.mapper.InteractionLogMapper;
import com.bqsummer.mapper.StageTransitionLogMapper;
import com.bqsummer.mapper.StageTransitionRuleMapper;
import com.bqsummer.mapper.UserRelationshipStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRelationshipStateService {

    private static final String SIGNAL_NATURAL_DECAY = "natural_decay";
    private static final String SIGNAL_SPECIAL_EMOTION = "special_emotion";
    private static final String SIGNAL_MANUAL_UPSERT = "manual_upsert";
    private static final String SIGNAL_MANUAL_INCREASE = "manual_increase";
    private static final String SIGNAL_MANUAL_DECREASE = "manual_decrease";

    private final UserRelationshipStateMapper userRelationshipStateMapper;
    private final StageTransitionLogMapper stageTransitionLogMapper;
    private final StageTransitionRuleMapper stageTransitionRuleMapper;
    private final InteractionLogMapper interactionLogMapper;
    private final RelationshipStageService relationshipStageService;
    private final Configs configs;

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
        insertInteractionLog(request.getUserId(), request.getAiCharacterId(), SIGNAL_MANUAL_UPSERT,
                null, delta, delta, null, request.getMeta(), now);

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

    @Transactional
    public UserRelationshipStateResponse applySignal(RelationshipInteractionSignalRequest request) {
        UserRelationshipState state = getOrInitState(request.getUserId(), request.getAiCharacterId());
        LocalDateTime now = LocalDateTime.now();

        applyNaturalDecayIfNeeded(state, now);

        int pointsRaw = resolvePointsRaw(request.getSignalType(), request.getValue(), request.getPointsRaw());
        int pointsApplied = pointsRaw;

        Map<String, Object> meta = copyMeta(request.getMeta());

        if (StringUtils.hasText(request.getWindowKey())) {
            Long duplicated = interactionLogMapper.countByWindowKey(
                    request.getUserId(), request.getAiCharacterId(), request.getSignalType(), request.getWindowKey());
            if (duplicated != null && duplicated > 0) {
                pointsApplied = 0;
                meta.put("droppedReason", "window_key_duplicated");
            }
        }

        if (pointsApplied > 0 && SIGNAL_SPECIAL_EMOTION.equals(request.getSignalType())) {
            int maxRewards = normalizePositiveConfig(configs.getSpecialEmotionMaxRewards(), 3);
            Long rewardedCount = interactionLogMapper.countAppliedBySignal(
                    request.getUserId(), request.getAiCharacterId(), SIGNAL_SPECIAL_EMOTION);
            if (rewardedCount != null && rewardedCount >= maxRewards) {
                pointsApplied = 0;
                meta.put("droppedReason", "special_emotion_reward_limit_reached");
            }
        }

        if (pointsApplied > 0) {
            pointsApplied = applyDailyCap(request.getUserId(), request.getAiCharacterId(), pointsApplied, meta, now);
        }

        int actualDelta = 0;
        if (pointsApplied != 0) {
            actualDelta = applyDeltaToState(
                    state,
                    pointsApplied,
                    request.getSignalType(),
                    meta,
                    now
            );
        }

        insertInteractionLog(
                request.getUserId(),
                request.getAiCharacterId(),
                request.getSignalType(),
                request.getValue(),
                pointsRaw,
                actualDelta,
                request.getWindowKey(),
                meta,
                now
        );

        if (actualDelta == 0) {
            state = userRelationshipStateMapper.findByUserAndCharacter(request.getUserId(), request.getAiCharacterId());
        }

        return toResponse(state);
    }

    private UserRelationshipStateResponse adjustScore(RelationshipScoreAdjustRequest request, boolean increase) {
        int delta = request.getDelta() == null ? 0 : request.getDelta();
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be > 0");
        }
        delta = increase ? delta : -delta;

        UserRelationshipState state = getOrInitState(request.getUserId(), request.getAiCharacterId());
        LocalDateTime now = LocalDateTime.now();

        applyNaturalDecayIfNeeded(state, now);

        int actualDelta = applyDeltaToState(state, delta, request.getReason(), request.getMeta(), now);

        insertInteractionLog(
                request.getUserId(),
                request.getAiCharacterId(),
                increase ? SIGNAL_MANUAL_INCREASE : SIGNAL_MANUAL_DECREASE,
                (double) Math.abs(request.getDelta()),
                delta,
                actualDelta,
                null,
                request.getMeta(),
                now
        );

        return toResponse(state);
    }

    private int applyDeltaToState(UserRelationshipState state,
                                  int delta,
                                  String reason,
                                  Map<String, Object> meta,
                                  LocalDateTime now) {
        int oldScore = state.getStageScore() == null ? 0 : state.getStageScore();
        int newScore = Math.max(0, oldScore + delta);

        Integer fromStageId = state.getStageId();
        Integer toStageId = resolveStageAfterScoreChange(
                state.getUserId(), state.getAiCharacterId(), fromStageId, newScore, delta > 0, now);

        state.setStageScore(newScore);
        state.setStageId(toStageId);
        state.setUpdatedAt(now);
        userRelationshipStateMapper.updateById(state);

        int actualDelta = newScore - oldScore;
        insertTransitionLog(
                state.getUserId(),
                state.getAiCharacterId(),
                fromStageId,
                toStageId,
                reason,
                actualDelta,
                meta,
                now
        );
        return actualDelta;
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

        Integer targetStageId = matched.get(0).getToStageId();
        if (increase && !targetStageId.equals(currentStageId) && !isUpgradeCooldownReady(userId, aiCharacterId, now)) {
            return currentStageId;
        }
        return targetStageId;
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

    private boolean isUpgradeCooldownReady(Long userId,
                                           Long aiCharacterId,
                                           LocalDateTime now) {
        int cooldownHours = normalizePositiveConfig(configs.getUpgradeCooldownHours(), 24);
        if (cooldownHours <= 0) {
            return true;
        }

        List<StageTransitionLog> recentLogs = stageTransitionLogMapper.findRecentByUserAndCharacter(userId, aiCharacterId, 20);
        if (recentLogs == null || recentLogs.isEmpty()) {
            return true;
        }

        for (StageTransitionLog log : recentLogs) {
            if (log.getCreatedAt() == null || log.getFromStageId() == null || log.getToStageId() == null) {
                continue;
            }
            if (log.getFromStageId().equals(log.getToStageId())) {
                continue;
            }
            RelationshipStage fromStage = relationshipStageService.requireById(log.getFromStageId());
            RelationshipStage toStage = relationshipStageService.requireById(log.getToStageId());
            if (toStage.getLevel() > fromStage.getLevel()) {
                long hours = Duration.between(log.getCreatedAt(), now).toHours();
                return hours >= cooldownHours;
            }
        }
        return true;
    }

    private void applyNaturalDecayIfNeeded(UserRelationshipState state, LocalDateTime now) {
        int decayAfterDays = normalizePositiveConfig(configs.getDecayAfterInactiveDays(), 7);
        int decayPerDay = normalizePositiveConfig(configs.getDecayPointsPerDay(), 3);
        if (decayAfterDays <= 0 || decayPerDay <= 0) {
            return;
        }

        Long userId = state.getUserId();
        Long aiCharacterId = state.getAiCharacterId();

        LocalDateTime lastNonDecayAt = interactionLogMapper.findLastInteractionAtExcludingSignal(
                userId,
                aiCharacterId,
                SIGNAL_NATURAL_DECAY
        );
        if (lastNonDecayAt == null) {
            lastNonDecayAt = state.getUpdatedAt() != null ? state.getUpdatedAt() : state.getCreatedAt();
        }
        if (lastNonDecayAt == null) {
            return;
        }

        long inactiveDays = Duration.between(lastNonDecayAt, now).toDays();
        if (inactiveDays <= decayAfterDays) {
            return;
        }

        int targetDecay = (int) (inactiveDays - decayAfterDays) * decayPerDay;
        int alreadyDecay = zeroIfNull(interactionLogMapper.sumAbsNegativePointsBySignalSince(
                userId,
                aiCharacterId,
                SIGNAL_NATURAL_DECAY,
                lastNonDecayAt
        ));

        int extraDecay = targetDecay - alreadyDecay;
        if (extraDecay <= 0) {
            return;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("inactiveDays", inactiveDays);
        meta.put("decayAfterInactiveDays", decayAfterDays);
        meta.put("decayPointsPerDay", decayPerDay);
        meta.put("alreadyDecay", alreadyDecay);
        meta.put("targetDecay", targetDecay);

        int actualDelta = applyDeltaToState(state, -extraDecay, SIGNAL_NATURAL_DECAY, meta, now);
        insertInteractionLog(userId, aiCharacterId, SIGNAL_NATURAL_DECAY, (double) inactiveDays,
                -extraDecay, actualDelta, null, meta, now);
    }

    private int resolvePointsRaw(String signalType, Double value, Integer pointsRaw) {
        if (pointsRaw != null) {
            return pointsRaw;
        }

        double safeValue = value == null ? 1.0D : Math.max(0.0D, value);
        int scaled = (int) Math.round(safeValue * 10.0D);

        if ("return_visit_day".equals(signalType)) {
            return Math.max(4, scaled);
        }
        if ("return_visit_week".equals(signalType)) {
            return Math.max(8, scaled + 2);
        }
        if ("long_turns".equals(signalType)) {
            return Math.max(3, scaled / 2);
        }
        if ("affectionate_address".equals(signalType) || "we_pronoun".equals(signalType)) {
            return Math.max(2, scaled / 3);
        }
        if ("boundary_reject".equals(signalType)) {
            return -Math.max(4, scaled);
        }
        if (SIGNAL_SPECIAL_EMOTION.equals(signalType)) {
            return Math.max(6, scaled);
        }
        return scaled;
    }

    private int applyDailyCap(Long userId,
                              Long aiCharacterId,
                              int points,
                              Map<String, Object> meta,
                              LocalDateTime now) {
        int cap = normalizePositiveConfig(configs.getDailyPointsCap(), 30);
        if (cap <= 0) {
            meta.put("droppedReason", "daily_cap_disabled_or_zero");
            return 0;
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(StringUtils.hasText(configs.getDailyResetTimezone())
                    ? configs.getDailyResetTimezone()
                    : "Asia/Shanghai");
        } catch (Exception e) {
            zoneId = ZoneId.of("Asia/Shanghai");
        }

        ZonedDateTime nowInZone = now.atZone(zoneId);
        LocalDateTime dayStart = nowInZone.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        int used = zeroIfNull(interactionLogMapper.sumPositiveAppliedPointsInRange(userId, aiCharacterId, dayStart, dayEnd));
        int remained = Math.max(0, cap - used);
        int applied = Math.min(points, remained);

        meta.put("dailyCap", cap);
        meta.put("dailyUsed", used);
        meta.put("dailyRemained", remained);
        if (applied < points) {
            meta.put("capApplied", true);
        }

        return applied;
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

    private void insertInteractionLog(Long userId,
                                      Long aiCharacterId,
                                      String signalType,
                                      Double value,
                                      Integer pointsRaw,
                                      Integer pointsApplied,
                                      String windowKey,
                                      Map<String, Object> meta,
                                      LocalDateTime now) {
        InteractionLog log = new InteractionLog();
        log.setUserId(userId);
        log.setAiCharacterId(aiCharacterId);
        log.setSignalType(signalType);
        log.setValue(value);
        log.setPointsRaw(pointsRaw == null ? 0 : pointsRaw);
        log.setPointsApplied(pointsApplied == null ? 0 : pointsApplied);
        log.setWindowKey(windowKey);
        log.setMetaJson(meta);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        interactionLogMapper.insert(log);
    }

    private Map<String, Object> copyMeta(Map<String, Object> meta) {
        if (meta == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(meta);
    }

    private int normalizePositiveConfig(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(0, value);
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
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
