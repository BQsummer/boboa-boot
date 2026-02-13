package com.bqsummer.service;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.character.AiCharacterSetting;
import com.bqsummer.common.vo.req.chararcter.CreateAiCharacterReq;
import com.bqsummer.common.vo.req.chararcter.UpsertCharacterSettingReq;
import com.bqsummer.constant.UserType;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.AiCharacterSettingMapper;
import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiCharacterService {

    private final AiCharacterMapper aiCharacterMapper;
    private final AiCharacterSettingMapper settingMapper;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 创建 AI 人物
     * <p>
     * 功能：创建AI角色的同时自动创建关联的用户账户
     * 事务性：使用@Transactional确保AI角色和用户账户原子性创建
     * 失败时回滚，不留下孤儿数据
     * </p>
     *
     * @param req    创建AI角色的请求参数
     * @param userId 创建者的用户ID
     * @return 包含AI角色ID和关联用户ID的响应
     */
    @Transactional
    public ResponseEntity<?> createCharacter(CreateAiCharacterReq req, Long userId) {
        // 校验用户存在
        User user = userMapper.findById(userId);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
            return ResponseEntity.status(401).body("用户无效");
        }

        String visibility = StringUtils.hasText(req.getVisibility()) ? req.getVisibility().toUpperCase() : "PRIVATE";
        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE")) {
            return ResponseEntity.badRequest().body("visibility 仅支持 PUBLIC/PRIVATE");
        }

        // 1. 创建AI角色
        AiCharacter c = AiCharacter.builder()
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .author(req.getAuthor())
                .visibility(visibility)
                .status(req.getStatus() == null ? true : req.getStatus())
                .createdByUserId(userId)
                .isDeleted(false)
                .build();
        aiCharacterMapper.insert(c);

        // 2. 生成随机密码并BCrypt加密
        String randomPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        // 3. 创建关联的用户账户
        String username = "ai_character_" + c.getId();
        String email = "ai_character_" + c.getId() + "@system.internal";

        User aiUser = User.builder()
                .username(username)
                .email(email)
                .nickName(req.getName())
                .avatar(req.getImageUrl())
                .password(encodedPassword)
                .userType(UserType.AI.getCode())
                .status(1)
                .build();
        userMapper.insertWithType(aiUser);

        // 4. 更新AI角色的associatedUserId
        aiCharacterMapper.updateAssociatedUserId(c.getId(), aiUser.getId());

        // 5. 返回响应（包含AI角色ID和关联用户ID）
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", c.getId());
        resp.put("associatedUserId", aiUser.getId());
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
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
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
     * <p>
     * 功能：更新AI角色信息的同时自动同步到关联的用户账户
     * 事务性：使用@Transactional确保AI角色和用户信息同步更新
     * name变化时同步更新User.nickName，imageUrl变化时同步更新User.avatar
     * </p>
     *
     * @param id     AI角色ID
     * @param req    更新请求参数
     * @param userId 当前用户ID
     * @return 更新结果
     */
    @Transactional
    public ResponseEntity<?> updateCharacter(Long id, CreateAiCharacterReq req, Long userId) {
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (exist == null || Boolean.TRUE.equals(exist.getIsDeleted())) {
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

        // 更新AI角色
        AiCharacter toUpdate = AiCharacter.builder()
                .id(id)
                .name(req.getName())
                .imageUrl(req.getImageUrl())
                .author(req.getAuthor())
                .visibility(visibility)
                .status(req.getStatus())
                .build();
        aiCharacterMapper.update(toUpdate);

        // 同步更新关联的用户账户
        if (exist.getAssociatedUserId() != null) {
            // 检测name字段是否变化，如果变化则更新User.nickName
            if (req.getName() != null && !req.getName().equals(exist.getName())) {
                userMapper.updateNickName(exist.getAssociatedUserId(), req.getName());
            }

            // 检测imageUrl字段是否变化，如果变化则更新User.avatar
            if (req.getImageUrl() != null && !req.getImageUrl().equals(exist.getImageUrl())) {
                userMapper.updateAvatar(exist.getAssociatedUserId(), req.getImageUrl());
            }
        }

        return ResponseEntity.ok("更新成功");
    }

    /**
     * 软删除人物
     * <p>
     * 功能：删除AI角色时同时软删除关联的用户账户
     * 事务性：使用@Transactional确保AI角色和用户同步删除
     * 保留历史数据，只标记is_deleted=true
     * </p>
     *
     * @param id     AI角色ID
     * @param userId 当前用户ID
     * @return 删除结果
     */
    @Transactional
    public ResponseEntity<?> deleteCharacter(Long id, Long userId) {
        AiCharacter exist = aiCharacterMapper.findById(id);
        if (exist == null || Boolean.TRUE.equals(exist.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // 只有创建者可以删除
        if (!exist.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("只有创建者可以删除");
        }

        // 软删除AI角色
        aiCharacterMapper.softDelete(id);

        // 同步软删除关联的用户账户
        if (exist.getAssociatedUserId() != null) {
            userMapper.softDelete(exist.getAssociatedUserId());
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 获取个性化设置
     */
    public ResponseEntity<AiCharacterSetting> getCharacterSetting(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // 检查访问权限
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        AiCharacterSetting s = settingMapper.findByUserAndCharacter(userId, characterId);
        if (s == null) {
            s = settingMapper.findDefaultByCharacter(characterId);
        }
        return ResponseEntity.ok(s);
    }

    /**
     * 设置/更新个性化设置
     */
    public ResponseEntity<AiCharacterSetting> getCharacterSettingForUser(Long characterId, Long targetUserId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        AiCharacterSetting s = settingMapper.findByUserAndCharacter(targetUserId, characterId);
        return ResponseEntity.ok(s);
    }

    public ResponseEntity<List<AiCharacterSetting>> listCharacterSettings(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(settingMapper.listByCharacter(characterId));
    }

    public ResponseEntity<AiCharacterSetting> getDefaultCharacterSetting(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(settingMapper.findDefaultByCharacter(characterId));
    }

    public ResponseEntity<?> upsertCharacterSetting(Long characterId, UpsertCharacterSettingReq req, Long userId) {
        return upsertCharacterSettingForUser(characterId, req, userId, userId);
    }

    public ResponseEntity<?> upsertCharacterSettingForUser(Long characterId, UpsertCharacterSettingReq req, Long targetUserId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
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
                .userId(targetUserId)
                .characterId(characterId)
                .name(req.getName())
                .avatarUrl(req.getAvatarUrl())
                .memorialDay(memorialDay)
                .relationship(req.getRelationship())
                .background(req.getBackground())
                .language(req.getLanguage())
                .customParams(req.getCustomParams())
                .isDeleted(false)
                .build();
        settingMapper.upsert(s);
        return ResponseEntity.ok("保存成功");
    }

    /**
     * 删除个性化设置
     */
    public ResponseEntity<?> upsertDefaultCharacterSetting(Long characterId, UpsertCharacterSettingReq req, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        LocalDate memorialDay = null;
        if (StringUtils.hasText(req.getMemorialDay())) {
            try {
                memorialDay = LocalDate.parse(req.getMemorialDay());
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("绾康鏃ユ牸寮忓簲涓?yyyy-MM-dd");
            }
        }

        AiCharacterSetting s = AiCharacterSetting.builder()
                .userId(null)
                .characterId(characterId)
                .name(req.getName())
                .avatarUrl(req.getAvatarUrl())
                .memorialDay(memorialDay)
                .relationship(req.getRelationship())
                .background(req.getBackground())
                .language(req.getLanguage())
                .customParams(req.getCustomParams())
                .isDeleted(false)
                .build();

        int updated = settingMapper.updateDefault(s);
        if (updated == 0) {
            settingMapper.insertDefault(s);
        }
        return ResponseEntity.ok("淇濆瓨鎴愬姛");
    }

    public ResponseEntity<?> deleteCharacterSetting(Long characterId, Long userId) {
        return deleteCharacterSettingForUser(characterId, userId, userId);
    }

    public ResponseEntity<?> deleteCharacterSettingForUser(Long characterId, Long targetUserId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }

        // 检查访问权限
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("无权限访问该人物");
        }

        settingMapper.softDelete(targetUserId, characterId);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> deleteDefaultCharacterSetting(Long characterId, Long userId) {
        AiCharacter c = aiCharacterMapper.findById(characterId);
        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }
        if (!c.getVisibility().equals("PUBLIC") && !c.getCreatedByUserId().equals(userId)) {
            return ResponseEntity.status(403).body("鏃犳潈闄愯闂浜虹墿");
        }
        settingMapper.softDeleteDefault(characterId);
        return ResponseEntity.ok().build();
    }
}
