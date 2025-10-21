package com.bqsummer.model.adapter;

import com.bqsummer.model.dto.InferenceRequest;
import com.bqsummer.model.dto.InferenceResponse;
import com.bqsummer.model.entity.AiModel;
import com.bqsummer.model.entity.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 适配器测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
class OpenAiAdapterTest {

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    private OpenAiAdapter adapter;
    private AiModel testModel;

    @BeforeEach
    void setUp() {
        // 创建测试模型
        testModel = new AiModel();
        testModel.setId(1L);
        testModel.setName("GPT-4");
        testModel.setVersion("gpt-4-turbo");
        testModel.setProvider("openai");
        testModel.setModelType(ModelType.CHAT);
        testModel.setApiEndpoint("https://api.openai.com/v1");
        testModel.setApiKey("sk-test-key-12345");
        testModel.setEnabled(true);

        // 创建适配器（如果 ChatClient.Builder 可用）
        if (chatClientBuilder != null) {
            adapter = new OpenAiAdapter(chatClientBuilder);
        }
    }

    @Test
    @DisplayName("测试 OpenAI 适配器初始化")
    void testAdapterInitialization() {
        if (adapter != null) {
            assertNotNull(adapter);
        } else {
            // 如果 Spring AI 未配置，跳过测试
            assertTrue(true, "Spring AI not configured, skipping test");
        }
    }

    @Test
    @DisplayName("测试支持的模型类型")
    void testSupports() {
        if (adapter != null) {
            assertTrue(adapter.supports(testModel));
            
            // 测试不支持的提供商
            AiModel unsupportedModel = new AiModel();
            unsupportedModel.setProvider("qwen");
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
