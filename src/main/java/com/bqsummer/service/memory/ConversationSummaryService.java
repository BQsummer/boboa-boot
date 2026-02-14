package com.bqsummer.service.memory;

import com.bqsummer.common.dto.memory.ConversationMessage;
import com.bqsummer.common.dto.memory.ConversationSummary;
import com.bqsummer.common.dto.memory.SummaryJson;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.mapper.memory.ConversationMessageMapper;
import com.bqsummer.mapper.memory.ConversationSummaryMapper;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.framework.exception.SnorlaxServerException;
import com.bqsummer.service.ai.UnifiedInferenceService;
import com.bqsummer.service.prompt.BeetlTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对话总结服务
 * 负责生成和管理对话总结，用于节省长对话的token消耗
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationSummaryService {

    private final ConversationSummaryMapper summaryMapper;
    private final ConversationMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final BeetlTemplateService beetlTemplateService;
    private final UnifiedInferenceService inferenceService;

    /**
     * 获取最新的总结
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @return 最新总结，如果没有则返回null
     */
    public ConversationSummary getLatestSummary(Long userId, Long aiCharacterId) {
        validateParameters(userId, aiCharacterId);
        return summaryMapper.findLatestSummary(userId, aiCharacterId);
    }

    /**
     * 获取最新的N条总结
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param limit         返回数量（1-10）
     * @return 总结列表（按创建时间降序）
     */
    public List<ConversationSummary> getLatestSummaries(Long userId, Long aiCharacterId, int limit) {
        validateParameters(userId, aiCharacterId);
        
        if (limit < 1 || limit > 10) {
            throw new SnorlaxClientException("limit必须在1-10之间");
        }
        
        return summaryMapper.findLatestSummaries(userId, aiCharacterId, limit);
    }

    /**
     * 异步生成对话总结
     * 当对话消息数达到阈值时触发此方法
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void generateSummaryAsync(Long userId, Long aiCharacterId) {
        try {
            log.info("开始生成对话总结: userId={}, aiCharacterId={}", userId, aiCharacterId);

            // 1. 获取最新总结，确定需要总结的消息范围
            ConversationSummary latestSummary = getLatestSummary(userId, aiCharacterId);
            Long startMessageId = latestSummary != null ? latestSummary.getCoveredUntilMessageId() : null;

            // 2. 获取需要总结的消息
            List<ConversationMessage> messagesToSummarize = getMessagesForSummary(userId, aiCharacterId, startMessageId);
            
            if (messagesToSummarize.isEmpty()) {
                log.warn("没有需要总结的消息: userId={}, aiCharacterId={}", userId, aiCharacterId);
                return;
            }

            // 3. 构造总结prompt
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("previousSummary", latestSummary != null ? latestSummary.getSummaryJson() : null);
            templateParams.put("messages", messagesToSummarize.stream()
                    .map(msg -> {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("senderType", msg.getSenderType());
                        msgMap.put("content", msg.getContent());
                        msgMap.put("createdAt", msg.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        return msgMap;
                    })
                    .collect(Collectors.toList()));

            String summaryPrompt = beetlTemplateService.renderFromResource(
                    "prompts/memory/summary-template.md",
                    templateParams
            );

            log.debug("总结prompt已生成，长度: {}", summaryPrompt.length());

            // 4. 调用LLM生成总结
            InferenceRequest inferenceRequest = new InferenceRequest();
            inferenceRequest.setPrompt(summaryPrompt);
            inferenceRequest.setTemperature(0.3); // 低温度以获得更稳定的输出
            inferenceRequest.setMaxTokens(1000);
            inferenceRequest.setUserId(userId);
            inferenceRequest.setSource("conversation-memory-summary");

            InferenceResponse llmResponse = inferenceService.chat(inferenceRequest);

            if (llmResponse == null || llmResponse.getContent() == null) {
                throw new SnorlaxServerException("LLM返回空响应");
            }

            log.debug("LLM总结已生成，内容长度: {}", llmResponse.getContent().length());

            // 5. 解析LLM输出为SummaryJson
            SummaryJson summaryJson = parseSummaryJsonFromLLM(llmResponse.getContent());

            // 6. 保存总结
            ConversationSummary summary = ConversationSummary.builder()
                    .userId(userId)
                    .aiCharacterId(aiCharacterId)
                    .summaryJson(summaryJson)
                    .coveredUntilMessageId(messagesToSummarize.get(messagesToSummarize.size() - 1).getId())
                    .messageCount(messagesToSummarize.size())
                    .createdAt(LocalDateTime.now())
                    .build();

            summaryMapper.insert(summary);
            log.info("✓ 总结生成成功: summaryId={}, userId={}, aiCharacterId={}, 覆盖{}条消息", 
                    summary.getId(), userId, aiCharacterId, summary.getMessageCount());

        } catch (Exception e) {
            log.warn("✗ 总结生成失败: userId={}, aiCharacterId={}, 原因: {}", 
                    userId, aiCharacterId, e.getMessage(), e);
            throw new SnorlaxServerException("生成对话总结失败: " + e.getMessage());
        }
    }

    /**
     * 获取需要总结的消息
     *
     * @param userId           用户ID
     * @param aiCharacterId    AI角色ID
     * @param startMessageId   起始消息ID（不包含），如果为null则从头开始
     * @return 消息列表（按时间升序）
     */
    private List<ConversationMessage> getMessagesForSummary(Long userId, Long aiCharacterId, Long startMessageId) {
        return messageMapper.findMessagesForSummary(userId, aiCharacterId, startMessageId);
    }

    /**
     * 从LLM输出中解析SummaryJson
     * 处理可能包含markdown代码块的响应
     *
     * @param llmOutput LLM输出内容
     * @return 解析后的SummaryJson对象
     */
    private SummaryJson parseSummaryJsonFromLLM(String llmOutput) {
        try {
            // 清理可能的markdown代码块标记
            String jsonContent = llmOutput.trim();
            
            // 移除```json和```标记
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            
            jsonContent = jsonContent.trim();
            
            // 解析JSON
            SummaryJson summaryJson = objectMapper.readValue(jsonContent, SummaryJson.class);
            
            log.debug("成功解析SummaryJson: topics={}, keyPoints={}",
                    summaryJson.getTopics() != null ? summaryJson.getTopics().size() : 0,
                    summaryJson.getKeyPoints() != null ? summaryJson.getKeyPoints().size() : 0);
            
            return summaryJson;
            
        } catch (JsonProcessingException e) {
            log.error("解析LLM输出失败: {}", llmOutput, e);
            throw new SnorlaxServerException("解析LLM总结输出失败: " + e.getMessage());
        }
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
