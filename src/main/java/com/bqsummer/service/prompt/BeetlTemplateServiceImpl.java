package com.bqsummer.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.exception.BeetlException;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Beetl 模板渲染服务实现
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BeetlTemplateServiceImpl implements BeetlTemplateService {

    private final GroupTemplate groupTemplate;

    @Override
    public String render(String templateContent, Map<String, Object> params) {
        if (templateContent == null) {
            throw new TemplateRenderException("模板内容不能为空");
        }

        try {
            // 获取模板
            Template template = groupTemplate.getTemplate(templateContent);
            
            // 绑定参数
            if (params != null && !params.isEmpty()) {
                template.binding(params);
            }
            
            // 渲染并返回结果
            String result = template.render();
            log.debug("模板渲染成功，输入长度: {}, 输出长度: {}", templateContent.length(), result.length());
            return result;
            
        } catch (BeetlException e) {
            int errorLine = e.token != null ? e.token.line : -1;
            log.error("模板渲染错误：行 {}，原因：{}", errorLine, e.getMessage());
            String errorMsg = String.format("模板渲染错误：行 %d，原因：%s", 
                    errorLine, e.getMessage());
            throw new TemplateRenderException(errorMsg, errorLine, e);
        } catch (Exception e) {
            log.error("模板渲染发生未知错误：{}", e.getMessage(), e);
            throw new TemplateRenderException("模板渲染发生未知错误：" + e.getMessage(), e);
        }
    }
}
