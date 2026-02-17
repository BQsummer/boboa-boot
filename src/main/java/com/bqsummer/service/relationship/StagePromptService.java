package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.StagePrompt;
import com.bqsummer.common.vo.req.relationship.StagePromptCreateRequest;
import com.bqsummer.common.vo.req.relationship.StagePromptQueryRequest;
import com.bqsummer.common.vo.req.relationship.StagePromptUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.StagePromptResponse;
import com.bqsummer.mapper.StagePromptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StagePromptService {

    private static final Set<String> ALLOWED_PROMPT_TYPES = Set.of("system", "opener", "reply", "safety");

    private final StagePromptMapper stagePromptMapper;
    private final RelationshipStageService relationshipStageService;

    @Transactional
    public StagePromptResponse create(StagePromptCreateRequest request) {
        String stageCode = normalizeStageCode(request.getStageCode());
        String promptType = normalizePromptType(request.getPromptType());
        relationshipStageService.requireByCode(stageCode);

        Integer maxVersion = stagePromptMapper.getMaxVersion(stageCode, promptType);
        int nextVersion = maxVersion == null ? 1 : maxVersion + 1;

        StagePrompt prompt = new StagePrompt();
        prompt.setStageCode(stageCode);
        prompt.setPromptType(promptType);
        prompt.setVersion(nextVersion);
        prompt.setContent(request.getContent());
        prompt.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        prompt.setCreatedAt(LocalDateTime.now());
        prompt.setUpdatedAt(LocalDateTime.now());
        stagePromptMapper.insert(prompt);

        return toResponse(prompt);
    }

    public IPage<StagePromptResponse> list(StagePromptQueryRequest request) {
        LambdaQueryWrapper<StagePrompt> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getStageCode())) {
            wrapper.eq(StagePrompt::getStageCode, normalizeStageCode(request.getStageCode()));
        }
        if (StringUtils.hasText(request.getPromptType())) {
            wrapper.eq(StagePrompt::getPromptType, normalizePromptType(request.getPromptType()));
        }
        if (request.getIsActive() != null) {
            wrapper.eq(StagePrompt::getIsActive, request.getIsActive());
        }
        wrapper.orderByDesc(StagePrompt::getCreatedAt);

        Page<StagePrompt> page = new Page<>(request.getPage(), request.getPageSize());
        return stagePromptMapper.selectPage(page, wrapper).convert(this::toResponse);
    }

    public StagePromptResponse getById(Long id) {
        StagePrompt prompt = requirePrompt(id);
        return toResponse(prompt);
    }

    @Transactional
    public StagePromptResponse update(Long id, StagePromptUpdateRequest request) {
        StagePrompt prompt = requirePrompt(id);
        if (request.getContent() != null) {
            prompt.setContent(request.getContent());
        }
        if (request.getIsActive() != null) {
            prompt.setIsActive(request.getIsActive());
        }
        prompt.setUpdatedAt(LocalDateTime.now());
        stagePromptMapper.updateById(prompt);
        return toResponse(prompt);
    }

    @Transactional
    public void delete(Long id) {
        requirePrompt(id);
        stagePromptMapper.deleteById(id);
    }

    private StagePrompt requirePrompt(Long id) {
        StagePrompt prompt = stagePromptMapper.selectById(id);
        if (prompt == null) {
            throw new IllegalArgumentException("stage prompt not found, id=" + id);
        }
        return prompt;
    }

    private String normalizeStageCode(String stageCode) {
        if (!StringUtils.hasText(stageCode)) {
            throw new IllegalArgumentException("stageCode is required");
        }
        return stageCode.trim().toLowerCase();
    }

    private String normalizePromptType(String promptType) {
        if (!StringUtils.hasText(promptType)) {
            throw new IllegalArgumentException("promptType is required");
        }
        String normalized = promptType.trim().toLowerCase();
        if (!ALLOWED_PROMPT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported promptType: " + promptType);
        }
        return normalized;
    }

    private StagePromptResponse toResponse(StagePrompt prompt) {
        StagePromptResponse response = new StagePromptResponse();
        response.setId(prompt.getId());
        response.setStageCode(prompt.getStageCode());
        response.setPromptType(prompt.getPromptType());
        response.setVersion(prompt.getVersion());
        response.setContent(prompt.getContent());
        response.setIsActive(prompt.getIsActive());
        response.setCreatedAt(prompt.getCreatedAt());
        response.setUpdatedAt(prompt.getUpdatedAt());
        return response;
    }
}
