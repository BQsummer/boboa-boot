package com.bqsummer.service.memory;

import com.bqsummer.common.dto.memory.LongTermMemory;
import com.bqsummer.common.dto.memory.MemoryItem;
import com.bqsummer.common.dto.memory.MemorySearchResult;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.framework.exception.SnorlaxServerException;
import com.bqsummer.mapper.memory.LongTermMemoryMapper;
import com.bqsummer.service.ai.UnifiedInferenceService;
import com.bqsummer.service.prompt.BeetlTemplateService;
import com.bqsummer.util.EmbeddingUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 长期记忆服务
 * 负责从对话中提取和管理长期记忆
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LongTermMemoryService {

    private final LongTermMemoryMapper memoryMapper;
    private final BeetlTemplateService beetlTemplateService;
    private final UnifiedInferenceService inferenceService;
    private final EmbeddingUtil embeddingUtil;
    private final ObjectMapper objectMapper;

    /**
     * 异步提取并保存长期记忆
     * 当用户发送消息时触发，判断是否需要提取记忆
     *
     * @param userId          用户ID
     * @param aiCharacterId   AI角色ID
     * @param userMessage     用户消息内容
     * @param sourceMessageId 来源消息ID
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void extractMemoryAsync(Long userId, Long aiCharacterId, String userMessage, Long sourceMessageId) {
        try {
            log.info("开始判断是否需要提取记忆: userId={}, aiCharacterId={}, messageId={}",
                    userId, aiCharacterId, sourceMessageId);

            // 1. 判断是否需要提取记忆
            boolean shouldExtract = judgeNeedExtraction(userMessage);

            if (!shouldExtract) {
                log.debug("消息不需要提取记忆: messageId={}", sourceMessageId);
                return;
            }

            log.info("消息需要提取记忆，开始提取: messageId={}", sourceMessageId);

            // 2. 提取记忆项
            List<MemoryItem> memoryItems = extractMemoryItems(userMessage);

            if (memoryItems.isEmpty()) {
                log.warn("LLM判断需要提取但未返回记忆项: messageId={}", sourceMessageId);
                return;
            }

            log.info("成功提取{}条记忆: messageId={}", memoryItems.size(), sourceMessageId);

            // 3. 为每个记忆生成embedding并保存
            List<LongTermMemory> memories = new ArrayList<>();

            for (MemoryItem item : memoryItems) {
                try {
                    // 生成embedding
                    float[] embedding = embeddingUtil.generateEmbedding(item.getText());
                    
                    if (embedding == null || embedding.length == 0) {
                        throw new SnorlaxServerException("Embedding API 返回空向量");
                    }

                    // 构建记忆实体
                    LongTermMemory memory = LongTermMemory.builder()
                            .userId(userId)
                            .aiCharacterId(aiCharacterId)
                            .text(item.getText())
                            .embedding(embedding)
                            .memoryType(item.getType())
                            .importance(item.getImportance())
                            .sourceMessageId(sourceMessageId)
                            .accessCount(0)
                            .lastAccessedAt(null)
                            .createdAt(LocalDateTime.now())
                            .build();

                    memories.add(memory);

                } catch (Exception e) {
                    log.error("生成记忆embedding失败，跳过此条: text={}", item.getText(), e);
                    // 继续处理其他记忆
                }
            }

            // 4. 批量保存记忆
            if (!memories.isEmpty()) {
                for (LongTermMemory memory : memories) {
                    memoryMapper.insert(memory);
                }
                log.info("✓ 记忆提取成功: userId={}, aiCharacterId={}, 提取{}条记忆, 来源messageId={}",
                        userId, aiCharacterId, memories.size(), sourceMessageId);
            } else {
                log.info("记忆提取完成但无内容: userId={}, aiCharacterId={}, messageId={}",
                        userId, aiCharacterId, sourceMessageId);
            }

        } catch (Exception e) {
            log.error("✗ 记忆提取失败: userId={}, aiCharacterId={}, messageId={}",
                    userId, aiCharacterId, sourceMessageId, e);
            // 异步任务失败不影响主流程
        }
    }

    /**
     * 判断消息是否需要提取记忆
     *
     * @param userMessage 用户消息
     * @return true表示需要提取
     */
    private boolean judgeNeedExtraction(String userMessage) {
        try {
            // 渲染判断prompt
            Map<String, Object> params = new HashMap<>();
            params.put("userMessage", userMessage);

            String judgePrompt = beetlTemplateService.renderFromResource(
                    "prompts/memory/memory-judge-template.md",
                    params
            );

            // 调用LLM判断
            InferenceRequest request = new InferenceRequest();
            request.setPrompt(judgePrompt);
            request.setTemperature(0.2); // 低温度，更确定性的判断
            request.setMaxTokens(200);
            request.setSource("memory-judge");

            InferenceResponse response = inferenceService.chat(request);

            if (response == null || response.getContent() == null) {
                log.warn("LLM判断返回空响应，默认不提取");
                return false;
            }

            // 解析JSON响应
            String jsonContent = cleanJsonFromLLM(response.getContent());
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            boolean shouldExtract = jsonNode.get("should_extract").asBoolean(false);
            String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "未提供原因";

            log.debug("记忆判断结果: shouldExtract={}, reason={}", shouldExtract, reason);

            return shouldExtract;

        } catch (Exception e) {
            log.error("判断是否需要提取记忆失败", e);
            return false; // 出错时默认不提取
        }
    }

    /**
     * 从消息中提取记忆项
     *
     * @param userMessage 用户消息
     * @return 记忆项列表
     */
    private List<MemoryItem> extractMemoryItems(String userMessage) {
        try {
            // 渲染提取prompt
            Map<String, Object> params = new HashMap<>();
            params.put("userMessage", userMessage);

            String extractPrompt = beetlTemplateService.renderFromResource(
                    "prompts/memory/memory-extract-template.md",
                    params
            );

            // 调用LLM提取
            InferenceRequest request = new InferenceRequest();
            request.setPrompt(extractPrompt);
            request.setTemperature(0.3); // 较低温度，保证一致性
            request.setMaxTokens(1000);
            request.setSource("memory-extract");

            InferenceResponse response = inferenceService.chat(request);

            if (response == null || response.getContent() == null) {
                log.warn("LLM提取返回空响应");
                return List.of();
            }

            // 解析JSON响应
            String jsonContent = cleanJsonFromLLM(response.getContent());
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            List<MemoryItem> items = new ArrayList<>();
            JsonNode memoriesNode = jsonNode.get("memories");

            if (memoriesNode != null && memoriesNode.isArray()) {
                for (JsonNode memNode : memoriesNode) {
                    MemoryItem item = MemoryItem.builder()
                            .text(memNode.get("text").asText())
                            .type(memNode.get("type").asText())
                            .importance(memNode.get("importance").floatValue())
                            .reason(memNode.has("reason") ? memNode.get("reason").asText() : "")
                            .build();
                    items.add(item);
                }
            }

            log.debug("成功解析{}条记忆项", items.size());
            return items;

        } catch (Exception e) {
            log.error("提取记忆项失败", e);
            return List.of();
        }
    }

    /**
     * 根据记忆类型查询记忆
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param memoryType    记忆类型
     * @param limit         限制数量
     * @return 记忆列表
     */
    public List<LongTermMemory> getMemoriesByType(Long userId, Long aiCharacterId, String memoryType, int limit) {
        validateParameters(userId, aiCharacterId);

        if (memoryType == null || memoryType.trim().isEmpty()) {
            throw new SnorlaxClientException("memoryType不能为空");
        }

        if (limit <= 0 || limit > 100) {
            throw new SnorlaxClientException("limit必须在1-100之间");
        }

        return memoryMapper.findByType(userId, aiCharacterId, memoryType, limit);
    }

    /**
     * 查询高重要性记忆
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param minImportance 最小重要性阈值
     * @param limit         限制数量
     * @return 记忆列表
     */
    public List<LongTermMemory> getHighImportanceMemories(Long userId, Long aiCharacterId, float minImportance, int limit) {
        validateParameters(userId, aiCharacterId);

        if (minImportance < 0 || minImportance > 1) {
            throw new SnorlaxClientException("minImportance必须在0-1之间");
        }

        if (limit <= 0 || limit > 100) {
            throw new SnorlaxClientException("limit必须在1-100之间");
        }

        return memoryMapper.findHighImportance(userId, aiCharacterId, minImportance, limit);
    }

    /**
     * 语义搜索记忆
     * 使用向量相似度搜索，并进行重排序
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param queryText     查询文本
     * @param topK          返回数量
     * @return 记忆搜索结果列表（按最终得分降序）
     */
    public List<MemorySearchResult> searchMemories(Long userId, Long aiCharacterId, String queryText, int topK) {
        validateParameters(userId, aiCharacterId);

        if (queryText == null || queryText.trim().isEmpty()) {
            throw new SnorlaxClientException("queryText不能为空");
        }

        if (topK <= 0 || topK > 50) {
            throw new SnorlaxClientException("topK必须在1-50之间");
        }

        try {
            long startTime = System.currentTimeMillis();

            // 1. 生成查询向量
            float[] queryEmbedding = embeddingUtil.generateEmbedding(queryText);

            // 2. 向量搜索获取候选记忆（取topK*2以便重排序）
            int candidateCount = Math.min(topK * 2, 100);
            List<LongTermMemory> candidates = memoryMapper.searchByEmbedding(
                    userId, aiCharacterId, queryEmbedding, candidateCount);

            if (candidates.isEmpty()) {
                log.debug("向量搜索未找到相似记忆: queryText={}", queryText);
                return List.of();
            }

            log.debug("向量搜索获得{}条候选记忆，耗时{}ms",
                    candidates.size(), System.currentTimeMillis() - startTime);

            // 3. 重排序：计算最终得分
            List<MemorySearchResult> results = candidates.stream()
                    .map(memory -> {
                        float similarity = calculateCosineSimilarity(memory.getEmbedding(), queryEmbedding);
                        float timeDecay = calculateTimeDecay(memory.getLastAccessedAt(), memory.getCreatedAt());
                        float finalScore = similarity * 0.6f + memory.getImportance() * 0.3f + timeDecay * 0.1f;

                        return MemorySearchResult.builder()
                                .memoryId(memory.getId())
                                .text(memory.getText())
                                .memoryType(memory.getMemoryType())
                                .importance(memory.getImportance())
                                .similarity(similarity)
                                .finalScore(finalScore)
                                .build();
                    })
                    .filter(result -> result.getFinalScore() > 0.5f) // 过滤低分结果
                    .sorted((r1, r2) -> Float.compare(r2.getFinalScore(), r1.getFinalScore())) // 降序排序
                    .limit(topK)
                    .collect(Collectors.toList());

            long totalTime = System.currentTimeMillis() - startTime;
//            log.info("✓ 记忆搜索完成: userId={}, aiCharacterId={}, 查询=\"{}\", 结果数={}, 总耗时{}ms (embedding:{}ms, search:{}ms, rerank:{}ms)",
//                    userId, aiCharacterId, queryText, results.size(), totalTime, embeddingTime, searchTime, totalTime - embeddingTime - searchTime);

            // 4. 异步更新访问时间
            results.forEach(result -> updateAccessTimeAsync(result.getMemoryId()));

            return results;

        } catch (Exception e) {
            log.error("搜索记忆失败: userId={}, aiCharacterId={}, queryText={}",
                    userId, aiCharacterId, queryText, e);
            throw new SnorlaxClientException("搜索记忆失败: " + e.getMessage());
        }
    }

    /**
     * 异步更新记忆访问时间
     *
     * @param memoryId 记忆ID
     */
    @Async
    public void updateAccessTimeAsync(Long memoryId) {
        try {
            memoryMapper.updateAccessTime(memoryId, LocalDateTime.now());
            log.debug("更新记忆访问时间: memoryId={}", memoryId);
        } catch (Exception e) {
            log.error("更新记忆访问时间失败: memoryId={}", memoryId, e);
            // 异步任务失败不影响主流程
        }
    }

    /**
     * 计算余弦相似度
     * 注意：pgvector的<=>运算符已经返回余弦距离，这里用于Java层计算
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度（0-1）
     */
    private float calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算时间衰减因子
     * 基于指数衰减：decay = e^(-days/30)
     *
     * @param lastAccessedAt 最后访问时间（可能为null）
     * @param createdAt      创建时间
     * @return 衰减因子（0-1）
     */
    private float calculateTimeDecay(LocalDateTime lastAccessedAt, LocalDateTime createdAt) {
        LocalDateTime referenceTime = lastAccessedAt != null ? lastAccessedAt : createdAt;
        long daysSinceAccess = Duration.between(referenceTime, LocalDateTime.now()).toDays();

        // 指数衰减：30天半衰期
        return (float) Math.exp(-daysSinceAccess / 30.0);
    }

    /**
     * 清理LLM输出中的markdown代码块标记
     */
    private String cleanJsonFromLLM(String llmOutput) {
        String jsonContent = llmOutput.trim();

        if (jsonContent.startsWith("```json")) {
            jsonContent = jsonContent.substring(7);
        } else if (jsonContent.startsWith("```")) {
            jsonContent = jsonContent.substring(3);
        }

        if (jsonContent.endsWith("```")) {
            jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
        }

        return jsonContent.trim();
    }

    /**
     * 验证参数
     */
    private void validateParameters(Long userId, Long aiCharacterId) {
        if (userId == null) {
            throw new SnorlaxClientException("userId不能为空");
        }
        if (aiCharacterId == null) {
            throw new SnorlaxClientException("aiCharacterId不能为空");
        }
    }
}
