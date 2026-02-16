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
 * Prompt 妯℃澘鏈嶅姟瀹炵幇绫?
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
        // 鍙傛暟鏍￠獙
        if (request.getCharId() == null) {
            throw new IllegalArgumentException("charId 涓嶈兘涓虹┖");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content 涓嶈兘涓虹┖");
        }

        // 鑾峰彇褰撳墠鏈€澶х増鏈彿
        Integer maxVersion = promptTemplateMapper.getMaxVersionByCharId(request.getCharId());
        int newVersion = (maxVersion == null) ? 1 : maxVersion + 1;

        // 濡傛灉宸插瓨鍦ㄧ増鏈紝灏嗘墍鏈夌増鏈爣璁颁负闈炴渶鏂?
        if (maxVersion != null) {
            promptTemplateMapper.markAllAsNotLatest(request.getCharId());
        }

        // 鍒涘缓鏂版ā鏉?
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
        template.setPostProcessPipelineId(request.getPostProcessPipelineId());
        template.setPostProcessConfig(request.getPostProcessConfig());
        template.setIsDeleted(false);
        template.setCreatedBy(String.valueOf(createdBy));
        template.setCreatedAt(LocalDateTime.now());

        // 淇濆瓨鍒版暟鎹簱
        promptTemplateMapper.insert(template);

        log.info("鍒涘缓 Prompt 妯℃澘鎴愬姛锛宑harId: {}, version: {}, id: {}",
                request.getCharId(), newVersion, template.getId());

        return convertToResponse(template);
    }

    public IPage<PromptTemplateResponse> list(PromptTemplateQueryRequest request) {
        // 鏋勫缓鏌ヨ鏉′欢
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();

        // 鎺掗櫎宸插垹闄ょ殑璁板綍
        queryWrapper.eq(PromptTemplate::getIsDeleted, false);

        // 鎸?charId 绛涢€?
        if (request.getCharId() != null) {
            queryWrapper.eq(PromptTemplate::getCharId, request.getCharId());
        }

        // 鎸夌姸鎬佺瓫閫?
        if (request.getStatus() != null) {
            queryWrapper.eq(PromptTemplate::getStatus, request.getStatus());
        }

        // 鍙煡璇㈡渶鏂扮増鏈?
        if (Boolean.TRUE.equals(request.getIsLatest())) {
            queryWrapper.eq(PromptTemplate::getIsLatest, true);
        }

        // 榛樿鎸夊垱寤烘椂闂撮檷搴忔帓搴?
        queryWrapper.orderByDesc(PromptTemplate::getCreatedAt);

        // 鍒嗛〉鏌ヨ
        Page<PromptTemplate> page = new Page<>(request.getPage(), request.getPageSize());
        IPage<PromptTemplate> resultPage = promptTemplateMapper.selectPage(page, queryWrapper);

        // 杞崲涓哄搷搴斿璞?
        return resultPage.convert(this::convertToResponse);
    }

    public PromptTemplateResponse getById(Long id) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("妯℃澘涓嶅瓨鍦ㄦ垨宸插垹闄わ紝id: " + id);
        }

        return convertToResponse(template);
    }

    @Transactional
    public PromptTemplateResponse update(Long id, PromptTemplateUpdateRequest request, Long updatedBy) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("妯℃澘涓嶅瓨鍦ㄦ垨宸插垹闄わ紝id: " + id);
        }

        // 鏇存柊瀛楁锛堝彧鏇存柊闈炵┖瀛楁锛?
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
        if (request.getPostProcessPipelineId() != null) {
            template.setPostProcessPipelineId(request.getPostProcessPipelineId());
        }
        if (request.getPostProcessConfig() != null) {
            template.setPostProcessConfig(request.getPostProcessConfig());
        }

        // 璁板綍鏇存柊淇℃伅
        template.setUpdatedBy(String.valueOf(updatedBy));
        template.setUpdatedAt(LocalDateTime.now());

        // 淇濆瓨鏇存柊
        promptTemplateMapper.updateById(template);

        log.info("鏇存柊 Prompt 妯℃澘鎴愬姛锛宨d: {}, updatedBy: {}", id, updatedBy);

        return convertToResponse(template);
    }

    @Transactional
    public void delete(Long id, Long deletedBy) {
        PromptTemplate template = promptTemplateMapper.selectById(id);

        if (template == null || Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new RuntimeException("妯℃澘涓嶅瓨鍦ㄦ垨宸插垹闄わ紝id: " + id);
        }

        // 閫昏緫鍒犻櫎
        template.setIsDeleted(true);
        template.setUpdatedBy(String.valueOf(deletedBy));
        template.setUpdatedAt(LocalDateTime.now());

        promptTemplateMapper.updateById(template);

        log.info("鍒犻櫎 Prompt 妯℃澘鎴愬姛锛宨d: {}, deletedBy: {}", id, deletedBy);
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
            throw new RuntimeException("妯℃澘涓嶅瓨鍦ㄦ垨宸插垹闄わ紝id: " + id);
        }

        return beetlTemplateService.render(template.getContent(), params);
    }

    /**
     * 鑾峰彇鎸囧畾瑙掕壊鐨勬渶鏂扮ǔ瀹氱増鏈ā鏉?
     * @param charId 瑙掕壊ID
     * @return 鏈€鏂扮ǔ瀹氱増鏈ā鏉匡紝涓嶅瓨鍦ㄥ垯杩斿洖null
     */
    public PromptTemplate getLatestByCharId(Long charId) {
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PromptTemplate::getCharId, charId)
                .eq(PromptTemplate::getIsLatest, true)
                .eq(PromptTemplate::getIsDeleted, false);
        
        PromptTemplate template = promptTemplateMapper.selectOne(queryWrapper);
        
        if (template != null) {
            log.info("鏌ヨ鍒拌鑹叉渶鏂扮ǔ瀹氭ā鏉? charId={}, templateId={}, version={}", 
                    charId, template.getId(), template.getVersion());
        } else {
            log.debug("瑙掕壊鏈厤缃ǔ瀹氭ā鏉? charId={}", charId);
        }
        
        return template;
    }

    /**
     * 灏嗗疄浣撹浆鎹负鍝嶅簲瀵硅薄
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
        response.setPostProcessPipelineId(template.getPostProcessPipelineId());
        response.setPostProcessConfig(template.getPostProcessConfig());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedBy(template.getUpdatedBy());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}



