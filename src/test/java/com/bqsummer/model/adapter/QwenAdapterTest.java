package com.bqsummer.model.adapter;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.service.ai.adapter.QwenAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qwen 适配器测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
class QwenAdapterTest {

    private QwenAdapter adapter;
    private AiModelBo testModel;

    @BeforeEach
    void setUp() {
        adapter = new QwenAdapter();

        // 创建 Qwen 测试模型
        testModel = new AiModelBo();
        testModel.setId(2L);
        testModel.setName("Qwen");
        testModel.setVersion("qwen-turbo");
        testModel.setProvider("qwen");
        testModel.setModelType(ModelType.CHAT);
        testModel.setApiEndpoint("https://dashscope.aliyuncs.com/api/v1");
        testModel.setApiKey("qwen-test-key-12345");
        testModel.setEnabled(true);
    }

    @Test
    @DisplayName("测试 Qwen 适配器支持的提供商")
    void testSupports() {
        assertTrue(adapter.supports(testModel));

        // 测试不支持的提供商
        AiModelBo unsupportedModel = new AiModelBo();
        unsupportedModel.setProvider("openai");
        assertFalse(adapter.supports(unsupportedModel));
    }

    @Test
    @DisplayName("测试 Qwen 请求格式")
    void testQwenRequestFormat() {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("你好，世界！");
        request.setModelId(2L);
        request.setTemperature(0.8);
        request.setMaxTokens(200);

        assertNotNull(request.getPrompt());
        assertEquals(2L, request.getModelId());
    }
}
