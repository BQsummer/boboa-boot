package com.bqsummer.service.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.common.vo.req.prompt.PromptTemplateCreateRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateQueryRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.bqsummer.common.vo.resp.prompt.PromptTemplateResponse;
import com.bqsummer.constant.GrayStrategy;
import com.bqsummer.constant.TemplateStatus;
import com.bqsummer.mapper.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Prompt 模板服务实现类
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final BeetlTemplateService beetlTemplateService;

    @Transactional
    public PromptTemplateResponse create(PromptTemplateCreateRequest request, Long createdBy) {
        // 参数校验
        if (request.getCharId() == null) {
            throw new IllegalArgumentException("charId 不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content 不能为空");
        }

        // 获取当前最大版本号
        Integer maxVersion = promptTemplateMapper.getMaxVersionByCharId(request.getCharId());
        int newVersion = (maxVersion == null) ? 1 : maxVersion + 1;

        // 如果已存在版本，将所有版本标记为非最新
        if (maxVersion != null) {
            promptTemplateMapper.markAllAsNotLatest(request.getCharId());
        }

        // 创建新模板
        PromptTemplate template = new PromptTemplate();
        template.setCharId(request.getCharId());
        template.setDescription(request.getDescription());
        template.setModelCode(request.getModelCode());
        template.setLang(request.getLang());
        template.setContent(request.getContent());
        template.setParamSchema(request.getParamSchema());
        template.setVersion(newVersion);
        template.setIsLatest(true);
        template.setStatus(TemplateStatus.DRAFT.getCode());
        template.setGrayStrategy(GrayStrategy.NONE.getCode());
        template.setGrayRatio(request.getGrayRatio());
        template.setGrayUserList(request.getGrayUserList());
        template.setPriority(request.getPriority());
        template.setTags(request.getTags());
        template.setIsDeleted(false);
        template.setCreatedBy(String.valueOf(createdBy));
        template.setCreatedAt(LocalDateTime.now());

        // 保存到数据库
        promptTemplateMapper.insert(template);

        log.info("创建 Prompt 模板成功，charId: {}, version: {}, id: {}",
                request.getCharId(), newVersion, template.getId());

        return convertToResponse(template);
    }

    public IPage<PromptTemplateResponse> list(PromptTemplateQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();

        // 排除已删除的记录
        queryWrapper.eq(PromptTemplate::getIsDeleted, false);

        // 按 charId 筛选
        if (request.getCharId() != null) {
            queryWrapper.eq(PromptTemplate::getCharId, request.getCharId());
        }

        // 按状态筛选
        if (request.getStatus() != null) {
            queryWrapper.eq(PromptTemplate::getStatus, request.getStatus());
        }

        // 只查询最新版本
        if (Boolean.TRUE.equals(request.getIsLatest())) {
            queryWrapper.eq(PromptTemplate::getIsLatest, true);
        }

        // 默认按创建时间降序排序
        queryWrapper.orderByDesc(PromptTemplate::getCreatedAt);

        // 分页查询
        Page<PromptTemplate> page = new Page<>(request.getPage(), request.getPageSize());
        IPage<PromptTemplate> resultPage = promptTemplateMapper.selectPage(page, queryWrapper);

        // 转换为响应对象
        return resultPage.convert(this::convertToResponse);
    }

    public PromptTemplateResponse getById(Long id) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("模板不存在或已删除，id: " + id);
        }

        return convertToResponse(template);
    }

    @Transactional
    public PromptTemplateResponse update(Long id, PromptTemplateUpdateRequest request, Long updatedBy) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("模板不存在或已删除，id: " + id);
        }

        // 更新字段（只更新非空字段）
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getModelCode() != null) {
            template.setModelCode(request.getModelCode());
        }
        if (request.getLang() != null) {
            template.setLang(request.getLang());
        }
        if (request.getContent() != null) {
            template.setContent(request.getContent());
        }
        if (request.getParamSchema() != null) {
            template.setParamSchema(request.getParamSchema());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        if (request.getGrayStrategy() != null) {
            template.setGrayStrategy(request.getGrayStrategy());
        }
        if (request.getGrayRatio() != null) {
            template.setGrayRatio(request.getGrayRatio());
        }
        if (request.getGrayUserList() != null) {
            template.setGrayUserList(request.getGrayUserList());
        }
        if (request.getPriority() != null) {
            template.setPriority(request.getPriority());
        }
        if (request.getTags() != null) {
            template.setTags(request.getTags());
        }

        // 记录更新信息
        template.setUpdatedBy(String.valueOf(updatedBy));
        template.setUpdatedAt(LocalDateTime.now());

        // 保存更新
        promptTemplateMapper.updateById(template);

        log.info("更新 Prompt 模板成功，id: {}, updatedBy: {}", id, updatedBy);

        return convertToResponse(template);
    }

    @Transactional
    public void delete(Long id, Long deletedBy) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("模板不存在或已删除，id: " + id);
        }

        // 逻辑删除
        template.setIsDeleted(true);
        template.setUpdatedBy(String.valueOf(deletedBy));
        template.setUpdatedAt(LocalDateTime.now());

        promptTemplateMapper.updateById(template);

        log.info("删除 Prompt 模板成功，id: {}, deletedBy: {}", id, deletedBy);
    }

    @Transactional
    public PromptTemplateResponse updateStatus(Long id, Integer status, Long updatedBy) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("template not found or deleted, id: " + id);
        }

        if (status == null || TemplateStatus.fromCode(status) == null) {
            throw new IllegalArgumentException("invalid template status: " + status);
        }

        template.setStatus(status);
        template.setUpdatedBy(String.valueOf(updatedBy));
        template.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.updateById(template);

        return convertToResponse(template);
    }

    @Transactional
    public PromptTemplateResponse enable(Long id, Long updatedBy) {
        return updateStatus(id, TemplateStatus.ENABLED.getCode(), updatedBy);
    }

    @Transactional
    public PromptTemplateResponse disable(Long id, Long updatedBy) {
        return updateStatus(id, TemplateStatus.DISABLED.getCode(), updatedBy);
    }

    public String render(Long id, Map<String, Object> params) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("模板不存在或已删除，id: " + id);
        }

        return beetlTemplateService.render(template.getContent(), params);
    }

    /**
     * 获取指定角色的最新稳定版本模板
     * @param charId 角色ID
     * @return 最新稳定版本模板，不存在则返回null
     */
    public PromptTemplate getLatestByCharId(Long charId) {
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PromptTemplate::getCharId, charId)
                .eq(PromptTemplate::getIsLatest, true)
                .eq(PromptTemplate::getIsDeleted, false);
        
        PromptTemplate template = promptTemplateMapper.selectOne(queryWrapper);
        
        if (template != null) {
            log.info("查询到角色最新稳定模板: charId={}, templateId={}, version={}", 
                    charId, template.getId(), template.getVersion());
        } else {
            log.debug("角色未配置稳定模板: charId={}", charId);
        }
        
        return template;
    }

    /**
     * 将实体转换为响应对象
     */
    private PromptTemplateResponse convertToResponse(PromptTemplate template) {
        PromptTemplateResponse response = new PromptTemplateResponse();
        response.setId(template.getId());
        response.setCharId(template.getCharId());
        response.setDescription(template.getDescription());
        response.setModelCode(template.getModelCode());
        response.setLang(template.getLang());
        response.setContent(template.getContent());
        response.setParamSchema(template.getParamSchema());
        response.setVersion(template.getVersion());
        response.setIsLatest(template.getIsLatest());
        response.setStatus(template.getStatus());
        response.setGrayStrategy(template.getGrayStrategy());
        response.setGrayRatio(template.getGrayRatio());
        response.setGrayUserList(template.getGrayUserList());
        response.setPriority(template.getPriority());
        response.setTags(template.getTags());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedBy(template.getUpdatedBy());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
