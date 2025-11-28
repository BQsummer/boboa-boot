package com.bqsummer.service.prompt;

import java.util.Map;

/**
 * Beetl 模板渲染服务接口
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
public interface BeetlTemplateService {

    /**
     * 渲染模板
     *
     * @param templateContent 模板内容（Beetl 语法）
     * @param params 渲染参数
     * @return 渲染后的内容
     * @throws TemplateRenderException 模板语法错误时抛出
     */
    String render(String templateContent, Map<String, Object> params);
}
