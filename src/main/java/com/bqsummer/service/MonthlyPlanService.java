package com.bqsummer.service;

import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.character.MonthlyPlan;
import com.bqsummer.common.vo.req.chararcter.CreateMonthlyPlanReq;
import com.bqsummer.common.vo.req.chararcter.UpdateMonthlyPlanReq;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.MonthlyPlanMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 月度计划服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyPlanService {

    private final MonthlyPlanMapper monthlyPlanMapper;
    private final AiCharacterMapper aiCharacterMapper;
    private final ObjectMapper objectMapper;

    /**
     * 固定日期规则正则：day=N，N ∈ [1, 31]
     */
    private static final Pattern DAY_RULE_FIXED = Pattern.compile("^day=([1-9]|[12][0-9]|3[01])$");

    /**
     * 相对日期规则正则：weekday=W,week=K，W ∈ [1,7]，K ∈ [1,5]
     */
    private static final Pattern DAY_RULE_RELATIVE = Pattern.compile("^weekday=[1-7],week=[1-5]$");

    private static final DateTimeFormatter TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 创建月度计划
     */
    public ResponseEntity<?> createPlan(CreateMonthlyPlanReq req, Long userId) {
        log.info("创建月度计划请求: userId={}, characterId={}, dayRule={}", userId, req.getCharacterId(), req.getDayRule());
        
        // 1. 校验虚拟人物存在且用户有权限
        ResponseEntity<?> permissionCheck = checkCharacterPermission(req.getCharacterId(), userId);
        if (permissionCheck != null) {
            log.warn("创建月度计划权限校验失败: userId={}, characterId={}", userId, req.getCharacterId());
            return permissionCheck;
        }

        // 2. 校验必填字段
        if (req.getCharacterId() == null) {
            return ResponseEntity.badRequest().body("虚拟人物ID不能为空");
        }
        if (!StringUtils.hasText(req.getDayRule())) {
            return ResponseEntity.badRequest().body("日期规则不能为空");
        }
        if (!StringUtils.hasText(req.getStartTime())) {
            return ResponseEntity.badRequest().body("开始时间不能为空");
        }
        if (req.getDurationMin() == null || req.getDurationMin() <= 0) {
            return ResponseEntity.badRequest().body("持续时间必须大于0");
        }
        if (!StringUtils.hasText(req.getAction())) {
            return ResponseEntity.badRequest().body("活动内容不能为空");
        }

        // 3. 校验日期规则格式
        if (!isValidDayRule(req.getDayRule())) {
            return ResponseEntity.badRequest().body("日期规则格式不正确，应为 day=N 或 weekday=W,week=K");
        }

        // 4. 解析时间
        LocalTime startTime = parseTime(req.getStartTime());
        if (startTime == null) {
            return ResponseEntity.badRequest().body("时间格式不正确，应为 HH:mm 或 HH:mm:ss");
        }

        // 5. 校验 JSON 格式
        if (StringUtils.hasText(req.getParticipants()) && !isValidJsonArray(req.getParticipants())) {
            return ResponseEntity.badRequest().body("参与者格式不正确，应为 JSON 数组");
        }
        if (StringUtils.hasText(req.getExtra()) && !isValidJsonObject(req.getExtra())) {
            return ResponseEntity.badRequest().body("扩展信息格式不正确，应为 JSON 对象");
        }

        // 6. 创建计划
        MonthlyPlan plan = MonthlyPlan.builder()
                .characterId(req.getCharacterId())
                .dayRule(req.getDayRule())
                .startTime(startTime)
                .durationMin(req.getDurationMin())
                .location(req.getLocation())
                .action(req.getAction())
                .participants(req.getParticipants())
                .extra(req.getExtra())
                .isDeleted(false)
                .build();
        monthlyPlanMapper.insert(plan);
        log.info("月度计划创建成功: planId={}, characterId={}", plan.getId(), req.getCharacterId());

        return ResponseEntity.ok(plan);
    }

    /**
     * 查询虚拟人物的所有计划
     */
    public ResponseEntity<?> listPlansByCharacterId(Long characterId, Long userId) {
        log.debug("查询月度计划列表: characterId={}, userId={}", characterId, userId);
        
        // 校验虚拟人物存在且用户有权限查看
        AiCharacter character = aiCharacterMapper.findById(characterId);
        if (character == null || Boolean.TRUE.equals(character.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // 检查权限：公共的或者自己创建的
        if (!"PUBLIC".equals(character.getVisibility()) && !character.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("无权限访问该虚拟人物");
        }

        List<MonthlyPlan> plans = monthlyPlanMapper.listByCharacterId(characterId);
        return ResponseEntity.ok(plans);
    }

    /**
     * 根据 ID 获取计划详情
     */
    public ResponseEntity<?> getPlanById(Long id, Long userId) {
        log.debug("查询月度计划详情: planId={}, userId={}", id, userId);
        
        MonthlyPlan plan = monthlyPlanMapper.findById(id);
        if (plan == null) {
            log.warn("月度计划不存在: planId={}", id);
            return ResponseEntity.notFound().build();
        }

        // 校验用户对关联的虚拟人物有查看权限
        AiCharacter character = aiCharacterMapper.findById(plan.getCharacterId());
        if (character == null || Boolean.TRUE.equals(character.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        if (!"PUBLIC".equals(character.getVisibility()) && !character.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("无权限访问该计划");
        }

        return ResponseEntity.ok(plan);
    }

    /**
     * 更新月度计划
     */
    public ResponseEntity<?> updatePlan(Long id, UpdateMonthlyPlanReq req, Long userId) {
        log.info("更新月度计划请求: planId={}, userId={}", id, userId);
        
        // 1. 查询计划是否存在
        MonthlyPlan exist = monthlyPlanMapper.findById(id);
        if (exist == null) {
            log.warn("更新月度计划失败，计划不存在: planId={}", id);
            return ResponseEntity.notFound().build();
        }

        // 2. 校验用户对关联的虚拟人物有修改权限
        ResponseEntity<?> permissionCheck = checkCharacterPermission(exist.getCharacterId(), userId);
        if (permissionCheck != null) {
            return permissionCheck;
        }

        // 3. 校验日期规则格式（如果提供）
        if (StringUtils.hasText(req.getDayRule()) && !isValidDayRule(req.getDayRule())) {
            return ResponseEntity.badRequest().body("日期规则格式不正确，应为 day=N 或 weekday=W,week=K");
        }

        // 4. 解析时间（如果提供）
        LocalTime startTime = null;
        if (StringUtils.hasText(req.getStartTime())) {
            startTime = parseTime(req.getStartTime());
            if (startTime == null) {
                return ResponseEntity.badRequest().body("时间格式不正确，应为 HH:mm 或 HH:mm:ss");
            }
        }

        // 5. 校验持续时间（如果提供）
        if (req.getDurationMin() != null && req.getDurationMin() <= 0) {
            return ResponseEntity.badRequest().body("持续时间必须大于0");
        }

        // 6. 校验 JSON 格式（如果提供）
        if (StringUtils.hasText(req.getParticipants()) && !isValidJsonArray(req.getParticipants())) {
            return ResponseEntity.badRequest().body("参与者格式不正确，应为 JSON 数组");
        }
        if (StringUtils.hasText(req.getExtra()) && !isValidJsonObject(req.getExtra())) {
            return ResponseEntity.badRequest().body("扩展信息格式不正确，应为 JSON 对象");
        }

        // 7. 更新计划
        MonthlyPlan toUpdate = MonthlyPlan.builder()
                .id(id)
                .dayRule(req.getDayRule())
                .startTime(startTime)
                .durationMin(req.getDurationMin())
                .location(req.getLocation())
                .action(req.getAction())
                .participants(req.getParticipants())
                .extra(req.getExtra())
                .build();
        monthlyPlanMapper.update(toUpdate);
        log.info("月度计划更新成功: planId={}", id);

        return ResponseEntity.ok("更新成功");
    }

    /**
     * 删除月度计划（软删除）
     */
    public ResponseEntity<?> deletePlan(Long id, Long userId) {
        log.info("删除月度计划请求: planId={}, userId={}", id, userId);
        
        // 1. 查询计划是否存在
        MonthlyPlan exist = monthlyPlanMapper.findById(id);
        if (exist == null) {
            log.warn("删除月度计划失败，计划不存在: planId={}", id);
            return ResponseEntity.notFound().build();
        }

        // 2. 校验用户对关联的虚拟人物有修改权限
        ResponseEntity<?> permissionCheck = checkCharacterPermission(exist.getCharacterId(), userId);
        if (permissionCheck != null) {
            log.warn("删除月度计划权限校验失败: planId={}, userId={}", id, userId);
            return permissionCheck;
        }

        // 3. 软删除
        monthlyPlanMapper.softDelete(id);
        log.info("月度计划删除成功: planId={}", id);

        return ResponseEntity.ok().build();
    }

    /**
     * 检查用户对虚拟人物的修改权限
     *
     * @param characterId 虚拟人物ID
     * @param userId      用户ID
     * @return null 表示有权限，否则返回错误响应
     */
    private ResponseEntity<?> checkCharacterPermission(Long characterId, Long userId) {
        AiCharacter character = aiCharacterMapper.findById(characterId);
        if (character == null || Boolean.TRUE.equals(character.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // 只有创建者可以管理计划
        if (!character.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("只有虚拟人物的创建者可以管理其计划");
        }

        return null;
    }

    /**
     * 验证日期规则格式
     */
    private boolean isValidDayRule(String dayRule) {
        return DAY_RULE_FIXED.matcher(dayRule).matches() || DAY_RULE_RELATIVE.matcher(dayRule).matches();
    }

    /**
     * 解析时间字符串
     */
    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER_HH_MM_SS);
        } catch (DateTimeParseException e1) {
            try {
                return LocalTime.parse(timeStr, TIME_FORMATTER_HH_MM);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /**
     * 验证 JSON 数组格式
     */
    private boolean isValidJsonArray(String json) {
        try {
            objectMapper.readTree(json);
            return json.trim().startsWith("[");
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 验证 JSON 对象格式
     */
    private boolean isValidJsonObject(String json) {
        try {
            objectMapper.readTree(json);
            return json.trim().startsWith("{");
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
