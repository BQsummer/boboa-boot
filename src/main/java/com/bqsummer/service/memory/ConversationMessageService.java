package com.bqsummer.service.memory;

import com.bqsummer.common.dto.memory.ConversationSummary;
import com.bqsummer.common.dto.memory.ConversationMessage;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.memory.ConversationMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对话消息服务
 * 读取 message 表中的对话消息，并驱动 summary/memory 逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private final ConversationMessageMapper messageMapper;
    private final ConversationSummaryService summaryService;
    private final LongTermMemoryService longTermMemoryService;

    private static final int SUMMARY_THRESHOLD = 30;

    /**
     * 记录消息事件（消息已先写入 message 表）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveMessage(Long messageId, Long userId, Long aiCharacterId, String senderType, String content) {
        validateParameters(messageId, userId, aiCharacterId, senderType, content);

        log.info("消息事件已记录: messageId={}, userId={}, aiCharacterId={}, senderType={}",
                messageId, userId, aiCharacterId, senderType);

        try {
            int unsummarizedCount = getUnsummarizedMessageCount(userId, aiCharacterId);
            if (unsummarizedCount >= SUMMARY_THRESHOLD) {
                log.info("未总结消息达到阈值，触发总结生成: userId={}, aiCharacterId={}, count={}",
                        userId, aiCharacterId, unsummarizedCount);
                summaryService.generateSummaryAsync(userId, aiCharacterId);
            }
        } catch (Exception e) {
            log.error("检查总结条件失败，但主流程继续: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
        }

        if ("USER".equals(senderType)) {
            try {
                longTermMemoryService.extractMemoryAsync(userId, aiCharacterId, content, messageId);
            } catch (Exception e) {
                log.error("触发记忆提取失败，但主流程继续: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
            }
        }

        return messageId;
    }

    public List<ConversationMessage> getRecentMessages(Long userId, Long aiCharacterId, int limit) {
        if (userId == null || aiCharacterId == null) {
            throw new SnorlaxClientException("userId和aiCharacterId不能为空");
        }
        if (limit <= 0 || limit > 100) {
            throw new SnorlaxClientException("limit必须在1-100之间");
        }
        return messageMapper.findRecentMessages(userId, aiCharacterId, limit);
    }

    public int getUnsummarizedMessageCount(Long userId, Long aiCharacterId) {
        ConversationSummary latestSummary = summaryService.getLatestSummary(userId, aiCharacterId);

        if (latestSummary == null) {
            Long totalCount = messageMapper.countMessages(userId, aiCharacterId);
            return totalCount == null ? 0 : totalCount.intValue();
        }

        Long count = messageMapper.countMessagesAfterId(userId, aiCharacterId, latestSummary.getCoveredUntilMessageId());
        return count == null ? 0 : count.intValue();
    }

    private void validateParameters(Long messageId, Long userId, Long aiCharacterId, String senderType, String content) {
        if (messageId == null) {
            throw new SnorlaxClientException("messageId不能为空");
        }
        if (userId == null) {
            throw new SnorlaxClientException("userId不能为空");
        }
        if (aiCharacterId == null) {
            throw new SnorlaxClientException("aiCharacterId不能为空");
        }
        if (!"USER".equals(senderType) && !"AI".equals(senderType)) {
            throw new SnorlaxClientException("senderType必须是USER或AI");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new SnorlaxClientException("content不能为空");
        }
    }
}
