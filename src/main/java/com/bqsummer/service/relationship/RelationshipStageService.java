package com.bqsummer.service.relationship;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import com.bqsummer.common.vo.req.relationship.RelationshipStageCreateRequest;
import com.bqsummer.common.vo.req.relationship.RelationshipStageQueryRequest;
import com.bqsummer.common.vo.req.relationship.RelationshipStageUpdateRequest;
import com.bqsummer.common.vo.resp.relationship.RelationshipStageResponse;
import com.bqsummer.mapper.RelationshipStageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelationshipStageService {

    private final RelationshipStageMapper relationshipStageMapper;

    @Transactional
    public RelationshipStageResponse create(RelationshipStageCreateRequest request) {
        String code = normalizeCode(request.getCode());
        ensureCodeAvailable(code, null);

        RelationshipStage stage = new RelationshipStage();
        stage.setCode(code);
        stage.setName(request.getName());
        stage.setLevel(request.getLevel());
        stage.setDescription(request.getDescription());
        stage.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        stage.setCreatedAt(LocalDateTime.now());
        stage.setUpdatedAt(LocalDateTime.now());
        relationshipStageMapper.insert(stage);
        return toResponse(stage);
    }

    public IPage<RelationshipStageResponse> list(RelationshipStageQueryRequest request) {
        LambdaQueryWrapper<RelationshipStage> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getCode())) {
            wrapper.like(RelationshipStage::getCode, request.getCode().trim().toLowerCase());
        }
        if (request.getIsActive() != null) {
            wrapper.eq(RelationshipStage::getIsActive, request.getIsActive());
        }
        wrapper.orderByAsc(RelationshipStage::getLevel).orderByAsc(RelationshipStage::getId);

        Page<RelationshipStage> page = new Page<>(request.getPage(), request.getPageSize());
        return relationshipStageMapper.selectPage(page, wrapper).convert(this::toResponse);
    }

    public RelationshipStageResponse getById(Integer id) {
        RelationshipStage stage = requireStage(id);
        return toResponse(stage);
    }

    @Transactional
    public RelationshipStageResponse update(Integer id, RelationshipStageUpdateRequest request) {
        RelationshipStage stage = requireStage(id);

        if (request.getName() != null) {
            stage.setName(request.getName());
        }
        if (request.getLevel() != null) {
            stage.setLevel(request.getLevel());
        }
        if (request.getDescription() != null) {
            stage.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            stage.setIsActive(request.getIsActive());
        }

        stage.setUpdatedAt(LocalDateTime.now());
        relationshipStageMapper.updateById(stage);
        return toResponse(stage);
    }

    @Transactional
    public void delete(Integer id) {
        requireStage(id);
        relationshipStageMapper.deleteById(id);
    }

    public RelationshipStage requireByCode(String code) {
        RelationshipStage stage = relationshipStageMapper.findByCode(normalizeCode(code));
        if (stage == null) {
            throw new IllegalArgumentException("relationship stage not found, code=" + code);
        }
        return stage;
    }

    public RelationshipStage requireById(Integer id) {
        return requireStage(id);
    }

    public RelationshipStage requireFirstActiveStage() {
        RelationshipStage stage = relationshipStageMapper.findFirstActiveStage();
        if (stage == null) {
            throw new IllegalStateException("no active relationship stage configured");
        }
        return stage;
    }

    private RelationshipStage requireStage(Integer id) {
        RelationshipStage stage = relationshipStageMapper.selectById(id);
        if (stage == null) {
            throw new IllegalArgumentException("relationship stage not found, id=" + id);
        }
        return stage;
    }

    private void ensureCodeAvailable(String code, Integer ignoreId) {
        RelationshipStage existing = relationshipStageMapper.findByCode(code);
        if (existing == null) {
            return;
        }
        if (ignoreId != null && ignoreId.equals(existing.getId())) {
            return;
        }
        throw new IllegalArgumentException("relationship stage code already exists: " + code);
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }
        return code.trim().toLowerCase();
    }

    private RelationshipStageResponse toResponse(RelationshipStage stage) {
        RelationshipStageResponse response = new RelationshipStageResponse();
        response.setId(stage.getId());
        response.setCode(stage.getCode());
        response.setName(stage.getName());
        response.setLevel(stage.getLevel());
        response.setDescription(stage.getDescription());
        response.setIsActive(stage.getIsActive());
        response.setCreatedAt(stage.getCreatedAt());
        response.setUpdatedAt(stage.getUpdatedAt());
        return response;
    }
}
