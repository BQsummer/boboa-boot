package com.bqsummer.service.memory;

import com.bqsummer.common.dto.memory.ConversationMessage;
import com.bqsummer.common.dto.memory.ConversationSummary;
import com.bqsummer.common.dto.memory.LongTermMemory;
import com.bqsummer.common.dto.memory.MemorySearchResult;
import com.bqsummer.common.dto.memory.SummaryJson;
import com.bqsummer.framework.exception.SnorlaxClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 记忆检索服务
 * 负责构建对话上下文，组合总结和最近消息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryRetrievalService {

    private final ConversationSummaryService summaryService;
    private final ConversationMessageService messageService;
    private final LongTermMemoryService longTermMemoryService;

    // 默认获取最近N条消息
    private static final int DEFAULT_RECENT_MESSAGE_COUNT = 10;
    // 默认高重要性阈值
    private static final float HIGH_IMPORTANCE_THRESHOLD = 0.7f;
    // 默认语义搜索数量
    private static final int DEFAULT_SEMANTIC_SEARCH_COUNT = 5;

    /**
     * 构建对话上下文prompt
     * 将总结内容和最近消息组合成易于LLM理解的格式
     *
     * @param userId           用户ID
     * @param aiCharacterId    AI角色ID
     * @param currentUserInput 当前用户输入
     * @param recentCount      包含的最近消息数（默认10条）
     * @return 格式化的上下文prompt
     */
    public String buildContextPrompt(Long userId, Long aiCharacterId, String currentUserInput, Integer recentCount) {
        validateParameters(userId, aiCharacterId);

        if (recentCount == null || recentCount <= 0) {
            recentCount = DEFAULT_RECENT_MESSAGE_COUNT;
        }

        if (recentCount > 50) {
            recentCount = 50; // 限制最大值
        }

        StringBuilder contextBuilder = new StringBuilder();

        // 1. 添加历史总结（如果存在）
        ConversationSummary latestSummary = summaryService.getLatestSummary(userId, aiCharacterId);
        if (latestSummary != null && latestSummary.getSummaryJson() != null) {
            contextBuilder.append("# 对话历史总结\n\n");
            appendSummaryContent(contextBuilder, latestSummary.getSummaryJson());
            contextBuilder.append("\n---\n\n");
        }

        // 2. 添加高重要性长期记忆
        try {
            List<LongTermMemory> importantMemories = longTermMemoryService.getHighImportanceMemories(
                    userId, aiCharacterId, HIGH_IMPORTANCE_THRESHOLD, 10);
            if (!importantMemories.isEmpty()) {
                contextBuilder.append("# 重要记忆\n\n");
                appendImportantMemories(contextBuilder, importantMemories);
                contextBuilder.append("\n---\n\n");
            }
        } catch (Exception e) {
            log.error("获取高重要性记忆失败: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
            // 失败不影响上下文构建
        }

        // 3. 添加最近的对话
        List<ConversationMessage> recentMessages = messageService.getRecentMessages(userId, aiCharacterId, recentCount);
        if (!recentMessages.isEmpty()) {
            contextBuilder.append("# 最近对话\n\n");
            appendRecentMessages(contextBuilder, recentMessages);
            contextBuilder.append("\n---\n\n");
        }

        // 4. 添加语义相关的长期记忆（如果提供了用户输入）
        if (currentUserInput != null && !currentUserInput.trim().isEmpty()) {
            try {
                List<MemorySearchResult> relevantMemories = longTermMemoryService.searchMemories(
                        userId, aiCharacterId, currentUserInput, DEFAULT_SEMANTIC_SEARCH_COUNT);
                if (!relevantMemories.isEmpty()) {
                    contextBuilder.append("# 相关记忆\n\n");
                    appendRelevantMemories(contextBuilder, relevantMemories);
                    contextBuilder.append("\n---\n\n");
                }
            } catch (Exception e) {
                log.error("语义搜索记忆失败: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
                // 失败不影响上下文构建
            }
        }

        // 5. 添加当前用户输入
        if (currentUserInput != null && !currentUserInput.trim().isEmpty()) {
            contextBuilder.append("# 当前用户输入\n\n");
            contextBuilder.append(currentUserInput.trim());
        }

        String context = contextBuilder.toString();
        log.debug("构建上下文完成: userId={}, aiCharacterId={}, contextLength={}",
                userId, aiCharacterId, context.length());

        return context;
    }

    /**
     * 构建对话上下文prompt（使用默认最近消息数）
     */
    public String buildContextPrompt(Long userId, Long aiCharacterId, String currentUserInput) {
        return buildContextPrompt(userId, aiCharacterId, currentUserInput, DEFAULT_RECENT_MESSAGE_COUNT);
    }

    /**
     * 添加总结内容到上下文
     */
    private void appendSummaryContent(StringBuilder builder, SummaryJson summary) {
        // 话题
        if (summary.getTopics() != null && !summary.getTopics().isEmpty()) {
            builder.append("**对话话题**:\n");
            for (SummaryJson.Topic topic : summary.getTopics()) {
                builder.append("- ").append(topic.getName()).append(": ").append(topic.getSummary()).append("\n");
            }
            builder.append("\n");
        }

        // 关键要点
        if (summary.getKeyPoints() != null && !summary.getKeyPoints().isEmpty()) {
            builder.append("**关键要点**:\n");
            for (String point : summary.getKeyPoints()) {
                builder.append("- ").append(point).append("\n");
            }
            builder.append("\n");
        }

        // 用户情绪
        if (summary.getUserEmotion() != null && !summary.getUserEmotion().isEmpty()) {
            builder.append("**用户情绪**: ").append(summary.getUserEmotion()).append("\n\n");
        }

        // 上下文延续
        if (summary.getContextCarryOver() != null && !summary.getContextCarryOver().isEmpty()) {
            builder.append("**待延续事项**: ").append(summary.getContextCarryOver()).append("\n");
        }
    }

    /**
     * 添加最近消息到上下文
     */
    private void appendRecentMessages(StringBuilder builder, List<ConversationMessage> messages) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // 消息按时间升序排列（最早的在前）
        List<ConversationMessage> sortedMessages = messages.stream()
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .toList();

        for (ConversationMessage msg : sortedMessages) {
            String time = msg.getCreatedAt().format(formatter);
            String speaker = "USER".equals(msg.getSenderType()) ? "用户" : "AI";
            builder.append(String.format("[%s] %s: %s\n", time, speaker, msg.getContent()));
        }
    }

    /**
     * 添加重要记忆到上下文
     */
    private void appendImportantMemories(StringBuilder builder, List<LongTermMemory> memories) {
        for (LongTermMemory memory : memories) {
            String typeLabel = getMemoryTypeLabel(memory.getMemoryType());
            builder.append(String.format("[%s] %s (重要性: %.1f)\n",
                    typeLabel, memory.getText(), memory.getImportance()));
        }
    }

    /**
     * 添加语义相关记忆到上下文
     */
    private void appendRelevantMemories(StringBuilder builder, List<MemorySearchResult> memories) {
        for (MemorySearchResult memory : memories) {
            String typeLabel = getMemoryTypeLabel(memory.getMemoryType());
            builder.append(String.format("[%s] %s (相似度: %.2f, 重要性: %.1f)\n",
                    typeLabel, memory.getText(), memory.getSimilarity(), memory.getImportance()));
        }
    }

    /**
     * 获取记忆类型的中文标签
     */
    private String getMemoryTypeLabel(String memoryType) {
        return switch (memoryType) {
            case "preference" -> "偏好";
            case "event" -> "事件";
            case "relationship" -> "关系";
            case "emotion" -> "情绪";
            case "fact" -> "事实";
            default -> "其他";
        };
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
