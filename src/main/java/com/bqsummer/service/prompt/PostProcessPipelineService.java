package com.bqsummer.service.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.prompt.PostProcessPipeline;
import com.bqsummer.common.dto.prompt.PostProcessStep;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineCreateRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineQueryRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessPipelineUpdateRequest;
import com.bqsummer.common.vo.req.prompt.PostProcessStepRequest;
import com.bqsummer.common.vo.resp.prompt.PostProcessPipelineResponse;
import com.bqsummer.common.vo.resp.prompt.PostProcessStepResponse;
import com.bqsummer.mapper.PostProcessPipelineMapper;
import com.bqsummer.mapper.PostProcessStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostProcessPipelineService {

    private final PostProcessPipelineMapper pipelineMapper;
    private final PostProcessStepMapper stepMapper;

    @Transactional
    public PostProcessPipelineResponse create(PostProcessPipelineCreateRequest request, Long createdBy) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name不能为空");
        }

        Integer maxVersion = pipelineMapper.getMaxVersionByName(request.getName());
        int newVersion = maxVersion == null ? 1 : maxVersion + 1;
        if (maxVersion != null) {
            pipelineMapper.markAllAsNotLatest(request.getName());
        }

        PostProcessPipeline pipeline = new PostProcessPipeline();
        pipeline.setName(request.getName());
        pipeline.setDescription(request.getDescription());
        pipeline.setLang(StringUtils.hasText(request.getLang()) ? request.getLang() : "zh-CN");
        pipeline.setModelCode(request.getModelCode());
        pipeline.setVersion(newVersion);
        pipeline.setIsLatest(true);
        pipeline.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        pipeline.setGrayStrategy(request.getGrayStrategy() == null ? 0 : request.getGrayStrategy());
        pipeline.setGrayRatio(request.getGrayRatio());
        pipeline.setGrayUserList(request.getGrayUserList());
        pipeline.setTags(request.getTags());
        pipeline.setCreatedBy(String.valueOf(createdBy));
        pipeline.setUpdatedBy(String.valueOf(createdBy));
        pipeline.setCreatedAt(LocalDateTime.now());
        pipeline.setUpdatedAt(LocalDateTime.now());
        pipeline.setIsDeleted(false);
        pipelineMapper.insert(pipeline);

        replaceSteps(pipeline.getId(), request.getSteps());
        return getById(pipeline.getId());
    }

    public IPage<PostProcessPipelineResponse> list(PostProcessPipelineQueryRequest request) {
        LambdaQueryWrapper<PostProcessPipeline> qw = new LambdaQueryWrapper<>();
        qw.eq(PostProcessPipeline::getIsDeleted, false);
        if (StringUtils.hasText(request.getName())) {
            qw.like(PostProcessPipeline::getName, request.getName().trim());
        }
        if (request.getStatus() != null) {
            qw.eq(PostProcessPipeline::getStatus, request.getStatus());
        }
        qw.orderByDesc(PostProcessPipeline::getCreatedAt);

        Page<PostProcessPipeline> page = new Page<>(request.getPage(), request.getPageSize());
        IPage<PostProcessPipeline> data = pipelineMapper.selectPage(page, qw);
        return data.convert(this::convert);
    }

    public PostProcessPipelineResponse getById(Long id) {
        PostProcessPipeline pipeline = pipelineMapper.selectById(id);
        if (pipeline == null || Boolean.TRUE.equals(pipeline.getIsDeleted())) {
            throw new RuntimeException("后处理流水线不存在，id: " + id);
        }
        return convert(pipeline);
    }

    @Transactional
    public PostProcessPipelineResponse update(Long id, PostProcessPipelineUpdateRequest request, Long updatedBy) {
        PostProcessPipeline pipeline = pipelineMapper.selectById(id);
        if (pipeline == null || Boolean.TRUE.equals(pipeline.getIsDeleted())) {
            throw new RuntimeException("后处理流水线不存在，id: " + id);
        }

        if (request.getDescription() != null) {
            pipeline.setDescription(request.getDescription());
        }
        if (request.getLang() != null) {
            pipeline.setLang(request.getLang());
        }
        if (request.getModelCode() != null) {
            pipeline.setModelCode(request.getModelCode());
        }
        if (request.getStatus() != null) {
            pipeline.setStatus(request.getStatus());
        }
        if (request.getGrayStrategy() != null) {
            pipeline.setGrayStrategy(request.getGrayStrategy());
        }
        if (request.getGrayRatio() != null) {
            pipeline.setGrayRatio(request.getGrayRatio());
        }
        if (request.getGrayUserList() != null) {
            pipeline.setGrayUserList(request.getGrayUserList());
        }
        if (request.getTags() != null) {
            pipeline.setTags(request.getTags());
        }
        pipeline.setUpdatedBy(String.valueOf(updatedBy));
        pipeline.setUpdatedAt(LocalDateTime.now());
        pipelineMapper.updateById(pipeline);

        if (request.getSteps() != null) {
            replaceSteps(id, request.getSteps());
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id, Long updatedBy) {
        PostProcessPipeline pipeline = pipelineMapper.selectById(id);
        if (pipeline == null || Boolean.TRUE.equals(pipeline.getIsDeleted())) {
            throw new RuntimeException("后处理流水线不存在，id: " + id);
        }
        pipeline.setIsDeleted(true);
        pipeline.setUpdatedBy(String.valueOf(updatedBy));
        pipeline.setUpdatedAt(LocalDateTime.now());
        pipelineMapper.updateById(pipeline);
    }

    @Transactional
    public List<PostProcessStepResponse> replaceSteps(Long pipelineId, List<PostProcessStepRequest> steps) {
        PostProcessPipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline == null || Boolean.TRUE.equals(pipeline.getIsDeleted())) {
            throw new RuntimeException("后处理流水线不存在，id: " + pipelineId);
        }

        stepMapper.deleteByPipelineId(pipelineId);
        if (CollectionUtils.isEmpty(steps)) {
            return List.of();
        }

        List<PostProcessStepRequest> sorted = new ArrayList<>(steps);
        sorted.sort(Comparator.comparing(PostProcessStepRequest::getStepOrder));
        for (PostProcessStepRequest request : sorted) {
            PostProcessStep step = new PostProcessStep();
            step.setPipelineId(pipelineId);
            step.setStepOrder(request.getStepOrder());
            step.setStepType(request.getStepType());
            step.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
            step.setConfig(request.getConfig());
            step.setOnFail(request.getOnFail() == null ? 0 : request.getOnFail());
            step.setPriority(request.getPriority() == null ? 0 : request.getPriority());
            step.setCreatedAt(LocalDateTime.now());
            step.setUpdatedAt(LocalDateTime.now());
            stepMapper.insert(step);
        }
        return listSteps(pipelineId);
    }

    public List<PostProcessStepResponse> listSteps(Long pipelineId) {
        LambdaQueryWrapper<PostProcessStep> qw = new LambdaQueryWrapper<>();
        qw.eq(PostProcessStep::getPipelineId, pipelineId)
                .orderByAsc(PostProcessStep::getStepOrder)
                .orderByDesc(PostProcessStep::getPriority)
                .orderByAsc(PostProcessStep::getId);
        List<PostProcessStep> steps = stepMapper.selectList(qw);
        return steps.stream().map(this::convert).toList();
    }

    private PostProcessPipelineResponse convert(PostProcessPipeline pipeline) {
        PostProcessPipelineResponse response = new PostProcessPipelineResponse();
        response.setId(pipeline.getId());
        response.setName(pipeline.getName());
        response.setDescription(pipeline.getDescription());
        response.setLang(pipeline.getLang());
        response.setModelCode(pipeline.getModelCode());
        response.setVersion(pipeline.getVersion());
        response.setIsLatest(pipeline.getIsLatest());
        response.setStatus(pipeline.getStatus());
        response.setGrayStrategy(pipeline.getGrayStrategy());
        response.setGrayRatio(pipeline.getGrayRatio());
        response.setGrayUserList(pipeline.getGrayUserList());
        response.setTags(pipeline.getTags());
        response.setSteps(listSteps(pipeline.getId()));
        response.setCreatedBy(pipeline.getCreatedBy());
        response.setUpdatedBy(pipeline.getUpdatedBy());
        response.setCreatedAt(pipeline.getCreatedAt());
        response.setUpdatedAt(pipeline.getUpdatedAt());
        return response;
    }

    private PostProcessStepResponse convert(PostProcessStep step) {
        PostProcessStepResponse response = new PostProcessStepResponse();
        response.setId(step.getId());
        response.setPipelineId(step.getPipelineId());
        response.setStepOrder(step.getStepOrder());
        response.setStepType(step.getStepType());
        response.setEnabled(step.getEnabled());
        response.setConfig(step.getConfig());
        response.setOnFail(step.getOnFail());
        response.setPriority(step.getPriority());
        response.setCreatedAt(step.getCreatedAt());
        response.setUpdatedAt(step.getUpdatedAt());
        return response;
    }
}
