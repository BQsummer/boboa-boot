package com.bqsummer.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.framework.exception.SnorlaxServerException;
import com.bqsummer.mapper.AiModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Embedding向量生成工具类
 * 支持缓存和批量生成
 * 使用Spring Cache抽象，可通过配置切换缓存实现（本地缓存/Redis）
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingUtil {
    
    private final AiModelMapper aiModelMapper;
    private final EncryptionUtil encryptionUtil;
    
    /**
     * 生成单个文本的embedding向量（带缓存）
     * 使用Spring Cache注解，缓存键为文本内容
     * 
     * @param text 文本内容
     * @return 向量数组（1536维）
     */
    @Cacheable(value = "embeddings", key = "#text")
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new SnorlaxServerException("文本内容不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        
        // 生成embedding
        float[] embedding = generateEmbeddingInternal(List.of(text)).get(0);
        
        long generationTime = System.currentTimeMillis() - startTime;
        log.info("Embedding生成完成: 文本长度={}, 耗时{}ms", text.length(), generationTime);
        
        return embedding;
    }
    
    /**
     * 批量生成embedding向量
     * 
     * @param texts 文本列表
     * @return 向量数组列表
     */
    public List<float[]> generateEmbeddingBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new SnorlaxServerException("文本列表不能为空");
        }
        
        // OpenAI embedding API支持批量（最多2048条）
        if (texts.size() > 2048) {
            throw new SnorlaxServerException("批量生成embedding最多支持2048条文本");
        }
        
        return generateEmbeddingInternal(texts);
    }
    
    /**
     * 内部方法：调用OpenAI API生成embedding
     */
    private List<float[]> generateEmbeddingInternal(List<String> texts) {
        try {
            // 1. 获取embedding模型
            AiModel model = getEmbeddingModel();
            
            // 2. 解密API Key
            String apiKey = encryptionUtil.decrypt(model.getApiKey());
            
            // 3. 创建OpenAI API实例
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey(new SimpleApiKey(apiKey))
                    .build();
            
            // 4. 配置embedding选项
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(model.getVersion())  // 例如: text-embedding-3-small
                    .build();
            
            // 5. 创建EmbeddingModel
            EmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi, org.springframework.ai.document.MetadataMode.ALL, options);
            
            // 6. 调用embedding API
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            
            // 7. 提取向量
            List<float[]> embeddings = response.getResults().stream()
                    .map(result -> result.getOutput())
                    .collect(Collectors.toList());
            
            log.info("Embedding生成成功: textCount={}, modelId={}, modelName={}", 
                    texts.size(), model.getId(), model.getName());
            
            return embeddings;
            
        } catch (Exception e) {
            log.error("Embedding生成失败", e);
            throw new SnorlaxServerException("Embedding生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取默认的embedding模型
     */
    private AiModel getEmbeddingModel() {
        List<AiModel> models = aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelType, ModelType.EMBEDDING)
                        .eq(AiModel::getEnabled, true)
                        .orderByDesc(AiModel::getUpdatedAt)
                        .last("LIMIT 1")
        );
        
        if (models.isEmpty()) {
            throw new SnorlaxServerException("未找到可用的Embedding模型，请先配置EMBEDDING类型的模型");
        }
        
        return models.get(0);
    }
}
