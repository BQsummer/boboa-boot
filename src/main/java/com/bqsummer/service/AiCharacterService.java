package com.bqsummer.service;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.character.AiCharacterSetting;
import com.bqsummer.common.vo.req.chararcter.CreateAiCharacterReq;
import com.bqsummer.common.vo.req.chararcter.UpsertCharacterSettingReq;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.AiCharacterSettingMapper;
import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiCharacterService {

    private final AiCharacterMapper aiCharacterMapper;
    private final AiCharacterSettingMapper settingMapper;
    private final UserMapper userMapper;

    /**
     * 创建 AI 人物
     */
    public ResponseEntity<?> createCharacter(CreateAiCharacterReq req, Long userId) {
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
     * 列出当前用户可见的人物
     */
    public List<AiCharacter> listVisibleCharacters(Long userId) {
        return aiCharacterMapper.listVisibleForUser(userId);
    }

    /**
     * 查看人物详情
     */
    public ResponseEntity<AiCharacter> getCharacterById(Long id, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(id);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 检查权限：公共的或者自己创建的
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(c);
    }

    /**
     * 更新人物信息
     */
    public ResponseEntity<?> updateCharacter(Long id, CreateAiCharacterReq req, Long userId) {
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (exist == null || (exist.getIsDeleted() != null && exist.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 只有创建者可以更新
        if (!exist.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("只有创建者可以修改");
        }

        String visibility = req.getVisibility();
        if (StringUtils.hasText(visibility)) {
            visibility = visibility.toUpperCase();
            if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE")) {
                return ResponseEntity.badRequest().body("visibility 仅支持 PUBLIC/PRIVATE");
            }
        }

        AiCharacter toUpdate = AiCharacter.builder()
                .id(id)
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .author(req.getAuthor())
                .visibility(visibility)
                .status(req.getStatus())
                .build();
        aiCharacterMapper.update(toUpdate);
        return ResponseEntity.ok("更新成功");
    }

    /**
     * 软删除人物
     */
    public ResponseEntity<?> deleteCharacter(Long id, Long userId) {
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (exist == null || (exist.getIsDeleted() != null && exist.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 只有创建者可以删除
        if (!exist.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("只有创建者可以删除");
        }

        aiCharacterMapper.softDelete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取个性化设置
     */
    public ResponseEntity<AiCharacterSetting> getCharacterSetting(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 检查访问权限
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        AiCharacterSetting s = settingMapper.findByUserAndCharacter(userId, characterId);
        return ResponseEntity.ok(s);
    }

    /**
     * 设置/更新个性化设置
     */
    public ResponseEntity<?> upsertCharacterSetting(Long characterId, UpsertCharacterSettingReq req, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 检查访问权限
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

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
                .characterId(characterId)
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
     * 删除个性化设置
     */
    public ResponseEntity<?> deleteCharacterSetting(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return ResponseEntity.notFound().build();
        }

        // 检查访问权限
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("无权限访问该人物");
        }

        settingMapper.softDelete(userId, characterId);
        return ResponseEntity.ok().build();
    }
}
