package com.bqsummer.controller;

import com.bqsummer.common.dto.AiCharacter;
import com.bqsummer.common.dto.AiCharacterSetting;
import com.bqsummer.common.dto.User;
import com.bqsummer.common.vo.req.CreateAiCharacterReq;
import com.bqsummer.common.vo.req.UpsertCharacterSettingReq;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.AiCharacterSettingMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/characters")
@RequiredArgsConstructor
public class AiCharacterController {

    private final AiCharacterMapper aiCharacterMapper;
    private final AiCharacterSettingMapper settingMapper;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Long currentUserId(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    private boolean isOwner(Long userId, AiCharacter c) {
        return c != null && c.getCreatedByUserId() != null && c.getCreatedByUserId().equals(userId);
    }

    private boolean isVisibleTo(Long userId, AiCharacter c) {
        if (c == null || c.getIsDeleted() != null && c.getIsDeleted() == 1) return false;
        if ("PUBLIC".equalsIgnoreCase(c.getVisibility())) return true;
        return isOwner(userId, c);
    }

    /**
     * 创建 AI 人物（默认归属当前用户，可设置 PUBLIC/PRIVATE）
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createCharacter(@RequestBody CreateAiCharacterReq req, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        // 校验用户存在
        User user = userMapper.findById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            return ResponseEntity.status(401).body("用户无效");
        }

        String visibility = StringUtils.hasText(req.getVisibility()) ? req.getVisibility().toUpperCase() : "PRIVATE";
        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE")) {
            return ResponseEntity.badRequest().body("visibility 仅支持 PUBLIC/PRIVATE");
        }

        AiCharacter c = AiCharacter.builder()
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .author(req.getAuthor())
                .visibility(visibility)
                .status(req.getStatus() == null ? 1 : req.getStatus())
                .createdByUserId(userId)
                .isDeleted(0)
                .build();
        aiCharacterMapper.insert(c);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", c.getId());
        return ResponseEntity.ok(resp);
    }

    /**
     * 列出当前用户可见的人物（公共或自己创建）
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AiCharacter>> listVisible(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        List<AiCharacter> list = aiCharacterMapper.listVisibleForUser(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * 查看人物详情（仅公共或自己创建的）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AiCharacter> getById(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        AiCharacter c = aiCharacterMapper.findById(id);
        if (!isVisibleTo(userId, c)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(c);
    }

    /**
     * 更新人物（仅创建者可更改）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody CreateAiCharacterReq req, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).body("未授权");
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (!isOwner(userId, exist)) return ResponseEntity.status(403).body("无权限");

        AiCharacter toUpdate = AiCharacter.builder()
                .id(id)
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .author(req.getAuthor())
                .visibility(req.getVisibility())
                .status(req.getStatus())
                .build();
        aiCharacterMapper.update(toUpdate);
        return ResponseEntity.ok("更新成功");
    }

    /**
     * 软删除人物（仅创建者可删除）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).body("未授权");
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (!isOwner(userId, exist)) return ResponseEntity.status(403).body("无权限");
        aiCharacterMapper.softDelete(id);
        return ResponseEntity.ok().build();
    }

    // ---------- 用户个性化设置 ----------

    /**
     * 获取当前用户对该人物的个性化设置
     */
    @GetMapping("/{id}/setting")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AiCharacterSetting> getSetting(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        AiCharacter c = aiCharacterMapper.findById(id);
        if (!isVisibleTo(userId, c)) return ResponseEntity.status(404).build();
        AiCharacterSetting s = settingMapper.findByUserAndCharacter(userId, id);
        return ResponseEntity.ok(s);
    }

    /**
     * 设置/更新 当前用户对该人物的个性化设置（upsert）
     */
    @PutMapping("/{id}/setting")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> upsertSetting(@PathVariable("id") Long id, @RequestBody UpsertCharacterSettingReq req, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).body("未授权");
        AiCharacter c = aiCharacterMapper.findById(id);
        if (!isVisibleTo(userId, c)) return ResponseEntity.status(404).body("人物不存在或不可见");

        LocalDate memorialDay = null;
        if (StringUtils.hasText(req.getMemorialDay())) {
            try {
                memorialDay = LocalDate.parse(req.getMemorialDay());
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("纪念日格式应为 yyyy-MM-dd");
            }
        }

        AiCharacterSetting s = AiCharacterSetting.builder()
                .userId(userId)
                .characterId(id)
                .name(req.getName())
                .avatarUrl(req.getAvatarUrl())
                .memorialDay(memorialDay)
                .relationship(req.getRelationship())
                .background(req.getBackground())
                .language(req.getLanguage())
                .customParams(req.getCustomParams())
                .isDeleted(0)
                .build();
        settingMapper.upsert(s);
        return ResponseEntity.ok("保存成功");
    }

    /**
     * 删除个人化设置（软删除）
     */
    @DeleteMapping("/{id}/setting")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteSetting(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) return ResponseEntity.status(401).body("未授权");
        AiCharacter c = aiCharacterMapper.findById(id);
        if (!isVisibleTo(userId, c)) return ResponseEntity.status(404).body("人物不存在或不可见");
        settingMapper.softDelete(userId, id);
        return ResponseEntity.ok().build();
    }
}

