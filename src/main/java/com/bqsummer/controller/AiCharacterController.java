package com.bqsummer.controller;

import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.character.AiCharacterSetting;
import com.bqsummer.common.vo.req.chararcter.CreateAiCharacterReq;
import com.bqsummer.common.vo.req.chararcter.UpsertCharacterSettingReq;
import com.bqsummer.service.AiCharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/ai/characters")
@RequiredArgsConstructor
public class AiCharacterController {

    private final AiCharacterService aiCharacterService;

    /**
     * 创建 AI 人物（默认归属当前用户，可设置 PUBLIC/PRIVATE）
     */
    @PostMapping
    public ResponseEntity<?> createCharacter(@RequestBody CreateAiCharacterReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userId == null) return ResponseEntity.status(401).body("未授权");

        return aiCharacterService.createCharacter(req, userId);
    }

    /**
     * 列出当前用户可见的人物（公共或自己创建）
     */
    @GetMapping
    public ResponseEntity<List<AiCharacter>> listVisible() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<AiCharacter> list = aiCharacterService.listVisibleCharacters(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * 查看人物详情（仅公共或自己创建的）
     */
    @GetMapping("/{id}")
    public ResponseEntity<AiCharacter> getById(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.getCharacterById(id, userId);
    }

    /**
     * 更新人物（仅创建者可更改）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody CreateAiCharacterReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.updateCharacter(id, req, userId);
    }

    /**
     * 软删除人物（仅创建者可删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.deleteCharacter(id, userId);
    }

    /**
     * 获取当前用户对该人物的个性化设置
     */
    @GetMapping("/{id}/setting")
    public ResponseEntity<AiCharacterSetting> getSetting(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.getCharacterSetting(id, userId);
    }

    /**
     * 设置/更新 当前用户对该人物的个性化设置（upsert）
     */
    @PutMapping("/{id}/setting")
    public ResponseEntity<?> upsertSetting(@PathVariable("id") Long id, @RequestBody UpsertCharacterSettingReq req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.upsertCharacterSetting(id, req, userId);
    }

    /**
     * 删除个人化设置（软删除）
     */
    @DeleteMapping("/{id}/setting")
    public ResponseEntity<?> deleteSetting(@PathVariable("id") Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return aiCharacterService.deleteCharacterSetting(id, userId);
    }
}
