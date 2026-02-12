package com.bqsummer.service.prompt;

import com.bqsummer.framework.exception.SnorlaxServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.exception.BeetlException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Beetl 模板渲染服务实现
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BeetlTemplateService {

    private final GroupTemplate groupTemplate;

    public String render(String templateContent, Map<String, Object> params) {
        if (templateContent == null) {
            throw new SnorlaxServerException("模板内容不能为空");
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
            throw new SnorlaxServerException(errorMsg);
        } catch (Exception e) {
            log.error("模板渲染发生未知错误：{}", e.getMessage(), e);
            throw new SnorlaxServerException("模板渲染发生未知错误：" + e.getMessage());
        }
    }

    /**
     * 从资源文件渲染模板
     *
     * @param resourcePath 资源路径（相对于classpath，例如："prompts/memory/summary-template.md"）
     * @param params       模板参数
     * @return 渲染结果
     */
    public String renderFromResource(String resourcePath, Map<String, Object> params) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new SnorlaxServerException("资源路径不能为空");
        }

        try {
            // 读取资源文件内容
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                throw new SnorlaxServerException("模板文件不存在: " + resourcePath);
            }

            String templateContent = StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );

            log.debug("从资源文件加载模板: {}, 内容长度: {}", resourcePath, templateContent.length());

            // 使用现有的render方法渲染模板
            return render(templateContent, params);

        } catch (IOException e) {
            log.error("读取模板文件失败: {}", resourcePath, e);
            throw new SnorlaxServerException("读取模板文件失败: " + resourcePath);
        }
    }
}
