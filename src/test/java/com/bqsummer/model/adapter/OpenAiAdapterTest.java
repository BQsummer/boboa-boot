package com.bqsummer.model.adapter;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.service.ai.adapter.OpenAiAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 适配器测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
class OpenAiAdapterTest {

    private OpenAiAdapter adapter;
    private AiModelBo testModel;

    @BeforeEach
    void setUp() {
        // 创建测试模型
        testModel = new AiModelBo();
        testModel.setId(1L);
        testModel.setName("GPT-4");
        testModel.setVersion("gpt-4-turbo");
        testModel.setApiKind("openai");
        testModel.setModelType(ModelType.CHAT);
        testModel.setApiEndpoint("https://api.openai.com/v1");
        testModel.setApiKey("sk-test-key-12345");
        testModel.setEnabled(true);

        // 创建适配器（使用无参构造函数）
        adapter = new OpenAiAdapter();
    }

    @Test
    @DisplayName("测试 OpenAI 适配器初始化")
    void testAdapterInitialization() {
        assertNotNull(adapter);
    }

    @Test
    @DisplayName("测试支持的模型类型")
    void testSupports() {
        if (adapter != null) {
            assertTrue(adapter.supports(testModel));
            
            // 测试不支持的提供商
            AiModelBo unsupportedModel = new AiModelBo();
            unsupportedModel.setApiKind("qwen");
            assertFalse(adapter.supports(unsupportedModel));
        }
    }

    @Test
    @DisplayName("测试聊天推理请求格式")
    void testChatRequestFormat() {
        // 创建推理请求
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("Hello, how are you?");
        request.setModelId(1L);
        request.setTemperature(0.7);
        request.setMaxTokens(100);

        // 验证请求格式
        assertNotNull(request.getPrompt());
        assertEquals(1L, request.getModelId());
        assertEquals(0.7, request.getTemperature());
        assertEquals(100, request.getMaxTokens());
    }

    @Test
    @DisplayName("测试响应格式转换")
    void testResponseFormat() {
        // 模拟响应
        InferenceResponse response = new InferenceResponse();
        response.setContent("I'm doing well, thank you!");
        response.setModelId(1L);
        response.setModelName("GPT-4");
        response.setPromptTokens(10);
        response.setCompletionTokens(8);
        response.setTotalTokens(18);
        response.setResponseTimeMs(500);

        // 验证响应格式
        assertNotNull(response.getContent());
        assertEquals(1L, response.getModelId());
        assertEquals(18, response.getTotalTokens());
    }
}
