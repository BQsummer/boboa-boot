package com.bqsummer.service.prompt;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Beetl 模板渲染服务单元测试
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@DisplayName("Beetl 模板渲染服务测试")
class BeetlTemplateServiceTest {

    private BeetlTemplateService beetlTemplateService;

    @BeforeEach
    void setUp() throws IOException {
        // 创建 GroupTemplate
        Configuration cfg = Configuration.defaultConfiguration();
        StringTemplateResourceLoader resourceLoader = new StringTemplateResourceLoader();
        GroupTemplate groupTemplate = new GroupTemplate(resourceLoader, cfg);
        
        // 创建服务实例
        beetlTemplateService = new BeetlTemplateServiceImpl(groupTemplate);
    }

    @Test
    @DisplayName("测试简单变量替换")
    void testSimpleVariableReplacement() {
        // 准备模板和参数
        String template = "你好，${name}！";
        Map<String, Object> params = new HashMap<>();
        params.put("name", "张三");

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("你好，张三！", result);
    }

    @Test
    @DisplayName("测试多个变量替换")
    void testMultipleVariableReplacement() {
        // 准备模板和参数
        String template = "你是 ${characterName}，请以 ${style} 的风格回复用户。";
        Map<String, Object> params = new HashMap<>();
        params.put("characterName", "小助手");
        params.put("style", "友好");

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("你是 小助手，请以 友好 的风格回复用户。", result);
    }

    @Test
    @DisplayName("测试空参数使用默认值")
    void testDefaultValue() {
        // 准备模板（使用 Beetl 的默认值语法）
        String template = "用户：${userName!'匿名用户'}";
        Map<String, Object> params = new HashMap<>();
        // 不传入 userName 参数

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("用户：匿名用户", result);
    }

    @Test
    @DisplayName("测试模板语法错误时抛出异常")
    void testTemplateErrorThrowsException() {
        // 准备包含语法错误的模板
        String template = "错误的模板 ${";
        Map<String, Object> params = new HashMap<>();

        // 验证抛出异常
        assertThrows(TemplateRenderException.class, () -> {
            beetlTemplateService.render(template, params);
        });
    }

    @Test
    @DisplayName("测试复杂对象参数")
    void testComplexObjectParameter() {
        // 准备模板和复杂参数
        String template = "用户 ${user.name} 的角色是 ${user.role}";
        Map<String, Object> user = new HashMap<>();
        user.put("name", "李四");
        user.put("role", "管理员");
        
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("用户 李四 的角色是 管理员", result);
    }

    @Test
    @DisplayName("测试空模板")
    void testEmptyTemplate() {
        // 准备空模板
        String template = "";
        Map<String, Object> params = new HashMap<>();

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("", result);
    }

    @Test
    @DisplayName("测试纯文本模板（无变量）")
    void testPlainTextTemplate() {
        // 准备纯文本模板
        String template = "这是一段纯文本，没有任何变量。";
        Map<String, Object> params = new HashMap<>();

        // 执行渲染
        String result = beetlTemplateService.render(template, params);

        // 验证结果
        assertEquals("这是一段纯文本，没有任何变量。", result);
    }

    @Test
    @DisplayName("测试 null 参数")
    void testNullParams() {
        // 准备模板
        String template = "简单文本";

        // 执行渲染（传入 null 参数）
        String result = beetlTemplateService.render(template, null);

        // 验证结果
        assertEquals("简单文本", result);
    }
}
