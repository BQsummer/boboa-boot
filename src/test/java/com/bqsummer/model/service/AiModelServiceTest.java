package com.bqsummer.model.service;

import com.bqsummer.common.vo.req.ai.ModelRegisterRequest;
import com.bqsummer.common.vo.req.ai.ModelQueryRequest;
import com.bqsummer.common.vo.resp.ai.ModelResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.exception.ModelValidationException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.service.ai.AiModelService;
import com.bqsummer.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模型服务测试
 *
 */
@SpringBootTest
@Transactional
class AiModelServiceTest {

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private EncryptionUtil encryptionUtil;

    private ModelRegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        // 准备有效的注册请求
        validRequest = new ModelRegisterRequest();
        validRequest.setName("GPT-4");
        validRequest.setVersion("gpt-4-turbo");
        validRequest.setProvider("openai");
        validRequest.setModelType(ModelType.CHAT);
        validRequest.setApiEndpoint("https://api.openai.com/v1");
        validRequest.setApiKey("sk-test-key-12345");
        validRequest.setContextLength(8192);
        validRequest.setParameterCount("175B");
        validRequest.setTags(Arrays.asList("fast", "powerful"));
        validRequest.setWeight(5);
        validRequest.setEnabled(true);
    }

    @Test
    @DisplayName("测试模型注册 - 成功场景")
    void testRegisterModel_Success() {
        // When: 注册新模型
        ModelResponse response = aiModelService.registerModel(validRequest, 1L);

        // Then: 验证注册成功
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("GPT-4", response.getName());
        assertEquals("gpt-4-turbo", response.getVersion());
        assertEquals("openai", response.getProvider());
        assertEquals(ModelType.CHAT, response.getModelType());
        assertTrue(response.getEnabled());
        
        // 验证 API Key 未在响应中返回
        assertNull(response.getApiKey());
    }

    @Test
    @DisplayName("测试模型注册 - API 密钥加密")
    void testRegisterModel_ApiKeyEncryption() {
        // When: 注册新模型
        ModelResponse response = aiModelService.registerModel(validRequest, 1L);

        // Then: 验证数据库中的 API Key 已加密
        AiModel savedModel = aiModelMapper.selectById(response.getId());
        assertNotNull(savedModel);
        assertNotEquals("sk-test-key-12345", savedModel.getApiKey());
        
        // 验证可以正确解密
        String decryptedKey = encryptionUtil.decrypt(savedModel.getApiKey());
        assertEquals("sk-test-key-12345", decryptedKey);
    }

    @Test
    @DisplayName("测试模型注册 - 唯一性约束")
    void testRegisterModel_UniquenessConstraint() {
        // Given: 已存在相同名称和版本的模型
        aiModelService.registerModel(validRequest, 1L);

        // When & Then: 再次注册应抛出异常
        assertThrows(ModelValidationException.class, () -> {
            aiModelService.registerModel(validRequest, 1L);
        });
    }

    @Test
    @DisplayName("测试模型注册 - 必填字段验证")
    void testRegisterModel_RequiredFieldsValidation() {
        // Given: 缺少必填字段的请求
        ModelRegisterRequest invalidRequest = new ModelRegisterRequest();
        invalidRequest.setName("GPT-4");
        // 缺少 version 和其他必填字段

        // When & Then: 应抛出验证异常
        assertThrows(ModelValidationException.class, () -> {
            aiModelService.registerModel(invalidRequest, 1L);
        });
    }

    @Test
    @DisplayName("测试模型列表查询 - 不带过滤条件")
    void testListModels_NoFilter() {
        // Given: 注册多个模型
        aiModelService.registerModel(validRequest, 1L);
        
        ModelRegisterRequest request2 = new ModelRegisterRequest();
        request2.setName("GPT-3.5");
        request2.setVersion("gpt-3.5-turbo");
        request2.setProvider("openai");
        request2.setModelType(ModelType.CHAT);
        request2.setApiEndpoint("https://api.openai.com/v1");
        request2.setApiKey("sk-test-key-67890");
        request2.setEnabled(true);
        aiModelService.registerModel(request2, 1L);

        // When: 查询模型列表
        ModelQueryRequest queryRequest = new ModelQueryRequest();
        queryRequest.setPage(1);
        queryRequest.setPageSize(10);
        
        List<ModelResponse> models = aiModelService.listModels(queryRequest);

        // Then: 验证返回结果
        assertNotNull(models);
        assertTrue(models.size() >= 2);
    }

    @Test
    @DisplayName("测试模型列表查询 - 按提供商过滤")
    void testListModels_FilterByProvider() {
        // Given: 注册不同提供商的模型
        aiModelService.registerModel(validRequest, 1L);

        ModelRegisterRequest qwenRequest = new ModelRegisterRequest();
        qwenRequest.setName("Qwen");
        qwenRequest.setVersion("qwen-turbo");
        qwenRequest.setProvider("qwen");
        qwenRequest.setModelType(ModelType.CHAT);
        qwenRequest.setApiEndpoint("https://api.qwen.com/v1");
        qwenRequest.setApiKey("qwen-key-12345");
        qwenRequest.setEnabled(true);
        aiModelService.registerModel(qwenRequest, 1L);

        // When: 按 provider 过滤
        ModelQueryRequest queryRequest = new ModelQueryRequest();
        queryRequest.setProvider("openai");
        queryRequest.setPage(1);
        queryRequest.setPageSize(10);
        
        List<ModelResponse> models = aiModelService.listModels(queryRequest);

        // Then: 只返回 openai 的模型
        assertNotNull(models);
        assertTrue(models.stream().allMatch(m -> "openai".equals(m.getProvider())));
    }

    @Test
    @DisplayName("测试模型列表查询 - 按类型过滤")
    void testListModels_FilterByModelType() {
        // Given: 注册不同类型的模型
        aiModelService.registerModel(validRequest, 1L);

        ModelRegisterRequest embeddingRequest = new ModelRegisterRequest();
        embeddingRequest.setName("Text-Embedding");
        embeddingRequest.setVersion("text-embedding-ada-002");
        embeddingRequest.setProvider("openai");
        embeddingRequest.setModelType(ModelType.EMBEDDING);
        embeddingRequest.setApiEndpoint("https://api.openai.com/v1");
        embeddingRequest.setApiKey("sk-embed-key-12345");
        embeddingRequest.setEnabled(true);
        aiModelService.registerModel(embeddingRequest, 1L);

        // When: 按 modelType 过滤
        ModelQueryRequest queryRequest = new ModelQueryRequest();
        queryRequest.setModelType(ModelType.CHAT);
        queryRequest.setPage(1);
        queryRequest.setPageSize(10);
        
        List<ModelResponse> models = aiModelService.listModels(queryRequest);

        // Then: 只返回 CHAT 类型的模型
        assertNotNull(models);
        assertTrue(models.stream().allMatch(m -> ModelType.CHAT.equals(m.getModelType())));
    }

    @Test
    @DisplayName("测试模型列表查询 - 按启用状态过滤")
    void testListModels_FilterByEnabled() {
        // Given: 注册启用和禁用的模型
        aiModelService.registerModel(validRequest, 1L);

        ModelRegisterRequest disabledRequest = new ModelRegisterRequest();
        disabledRequest.setName("Disabled-Model");
        disabledRequest.setVersion("v1.0");
        disabledRequest.setProvider("openai");
        disabledRequest.setModelType(ModelType.CHAT);
        disabledRequest.setApiEndpoint("https://api.openai.com/v1");
        disabledRequest.setApiKey("sk-disabled-key");
        disabledRequest.setEnabled(false);
        aiModelService.registerModel(disabledRequest, 1L);

        // When: 只查询启用的模型
        ModelQueryRequest queryRequest = new ModelQueryRequest();
        queryRequest.setEnabled(true);
        queryRequest.setPage(1);
        queryRequest.setPageSize(10);
        
        List<ModelResponse> models = aiModelService.listModels(queryRequest);

        // Then: 只返回启用的模型
        assertNotNull(models);
        assertTrue(models.stream().allMatch(ModelResponse::getEnabled));
    }

    @Test
    @DisplayName("测试模型列表查询 - 分页")
    void testListModels_Pagination() {
        // Given: 注册多个模型
        for (int i = 1; i <= 5; i++) {
            ModelRegisterRequest request = new ModelRegisterRequest();
            request.setName("Model-" + i);
            request.setVersion("v1.0");
            request.setProvider("openai");
            request.setModelType(ModelType.CHAT);
            request.setApiEndpoint("https://api.openai.com/v1");
            request.setApiKey("sk-key-" + i);
            request.setEnabled(true);
            aiModelService.registerModel(request, 1L);
        }

        // When: 分页查询（每页2条）
        ModelQueryRequest queryRequest = new ModelQueryRequest();
        queryRequest.setPage(1);
        queryRequest.setPageSize(2);
        
        List<ModelResponse> page1 = aiModelService.listModels(queryRequest);

        // Then: 验证分页结果
        assertNotNull(page1);
        assertEquals(2, page1.size());
    }
}
