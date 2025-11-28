package com.bqsummer.configuration;

import lombok.extern.slf4j.Slf4j;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Beetl 模板引擎配置类
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Slf4j
@org.springframework.context.annotation.Configuration
public class BeetlConfiguration {

    /**
     * 创建 GroupTemplate Bean
     * 
     * 使用 StringTemplateResourceLoader 处理字符串模板，
     * GroupTemplate 是线程安全的，可作为单例使用。
     *
     * @return GroupTemplate 实例
     * @throws IOException 配置加载失败时抛出
     */
    @Bean
    public GroupTemplate groupTemplate() throws IOException {
        // 使用默认配置
        Configuration cfg = Configuration.defaultConfiguration();
        
        // 使用 StringTemplateResourceLoader 处理字符串模板
        StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();
        
        // 创建 GroupTemplate
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        
        log.info("Beetl 模板引擎初始化完成，定界符：${} 和 <% %>");
        return gt;
    }
}
