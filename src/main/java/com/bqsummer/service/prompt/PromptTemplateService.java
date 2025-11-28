package com.bqsummer.service.prompt;

import com.bqsummer.common.vo.req.prompt.PromptTemplateCreateRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateQueryRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.bqsummer.common.vo.resp.prompt.PromptTemplateResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

/**
 * Prompt 模板服务接口
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
public interface PromptTemplateService {

    /**
     * 创建 Prompt 模板
     * 
     * <p>自动递增版本号，新创建的模板标记为最新版本</p>
     *
     * @param request 创建请求
     * @param createdBy 创建人ID
     * @return 创建后的模板信息
     */
    PromptTemplateResponse create(PromptTemplateCreateRequest request, String createdBy);

    /**
     * 分页查询模板列表
     *
     * @param request 查询请求（包含分页和筛选条件）
     * @return 分页结果
     */
    IPage<PromptTemplateResponse> list(PromptTemplateQueryRequest request);

    /**
     * 根据ID查询模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     * @throws RuntimeException 模板不存在或已删除时抛出
     */
    PromptTemplateResponse getById(Long id);

    /**
     * 更新模板
     *
     * @param id 模板ID
     * @param request 更新请求
     * @param updatedBy 更新人ID
     * @return 更新后的模板信息
     * @throws RuntimeException 模板不存在时抛出
     */
    PromptTemplateResponse update(Long id, PromptTemplateUpdateRequest request, String updatedBy);

    /**
     * 删除模板（逻辑删除）
     *
     * @param id 模板ID
     * @param deletedBy 删除人ID
     * @throws RuntimeException 模板不存在时抛出
     */
    void delete(Long id, String deletedBy);

    /**
     * 渲染模板预览
     *
     * @param id 模板ID
     * @param params 渲染参数
     * @return 渲染后的内容
     * @throws TemplateRenderException 渲染失败时抛出
     */
    String render(Long id, Map<String, Object> params);
}
