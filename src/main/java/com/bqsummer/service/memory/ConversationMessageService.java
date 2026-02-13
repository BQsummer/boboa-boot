package com.bqsummer.service.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.dto.memory.ConversationMessage;
import com.bqsummer.common.dto.memory.ConversationSummary;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.memory.ConversationMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话消息服务
 * 负责消息的保存和查询
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageService {
    
    private final ConversationMessageMapper messageMapper;
    private final ConversationSummaryService summaryService;
    private final LongTermMemoryService longTermMemoryService;
    
    // 默认总结阈值：30条消息
    private static final int SUMMARY_THRESHOLD = 30;
    
    /**
     * 保存消息
     * 
     * @param userId 用户ID
     * @param aiCharacterId AI角色ID
     * @param senderType 发送者类型：USER 或 AI
     * @param content 消息内容
     * @return 消息ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveMessage(Long userId, Long aiCharacterId, String senderType, String content) {
        // 1. 参数校验
        validateParameters(userId, aiCharacterId, senderType, content);
        
        // 校验 sender_type 必须是 USER 或 AI
        if (!"USER".equals(senderType) && !"AI".equals(senderType)) {
            throw new SnorlaxClientException("sender_type 必须是 USER 或 AI，当前值: " + senderType);
        }
        
        // 2. 构建消息实体
        ConversationMessage message = ConversationMessage.builder()
                .userId(userId)
                .aiCharacterId(aiCharacterId)
                .senderType(senderType)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 3. 保存到数据库
        messageMapper.insert(message);
        
        log.info("消息已保存: messageId={}, userId={}, aiCharacterId={}, senderType={}", 
                message.getId(), userId, aiCharacterId, senderType);
        
        // 4. 检查是否需要生成总结
        try {
            int unsummarizedCount = getUnsummarizedMessageCount(userId, aiCharacterId);
            if (unsummarizedCount >= SUMMARY_THRESHOLD) {
                log.info("未总结消息数达到阈值，触发总结生成: userId={}, aiCharacterId={}, count={}", 
                        userId, aiCharacterId, unsummarizedCount);
                summaryService.generateSummaryAsync(userId, aiCharacterId);
            }
        } catch (Exception e) {
            log.error("检查总结条件失败，但消息已保存: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
            // 不影响主流程
        }
        
        // 5. 如果是用户消息，异步判断是否需要提取记忆
        if ("USER".equals(senderType)) {
            try {
                longTermMemoryService.extractMemoryAsync(userId, aiCharacterId, content, message.getId());
            } catch (Exception e) {
                log.error("触发记忆提取失败，但消息已保存: userId={}, aiCharacterId={}", userId, aiCharacterId, e);
                // 不影响主流程
            }
        }
        
        return message.getId();
    }
    
    /**
     * 获取最近的消息
     * 
     * @param userId 用户ID
     * @param aiCharacterId AI角色ID
     * @param limit 限制数量
     * @return 消息列表（按时间倒序）
     */
    public List<ConversationMessage> getRecentMessages(Long userId, Long aiCharacterId, int limit) {
        if (userId == null || aiCharacterId == null) {
            throw new SnorlaxClientException("userId和aiCharacterId不能为空");
        }
        
        if (limit <= 0 || limit > 100) {
            throw new SnorlaxClientException("limit必须在1-100之间");
        }
        
        return messageMapper.findRecentMessages(userId, aiCharacterId, limit);
    }
    
    /**
     * 获取未被总结覆盖的消息数量
     * 
     * @param userId 用户ID
     * @param aiCharacterId AI角色ID
     * @return 未总结消息数量
     */
    public int getUnsummarizedMessageCount(Long userId, Long aiCharacterId) {
        // 1. 获取最后一次总结
        ConversationSummary latestSummary = summaryService.getLatestSummary(userId, aiCharacterId);
        
        // 2. 如果没有总结，则所有消息都未被总结
        if (latestSummary == null) {
            Long totalCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<ConversationMessage>()
                            .eq(ConversationMessage::getUserId, userId)
                            .eq(ConversationMessage::getAiCharacterId, aiCharacterId)
            );
            return totalCount.intValue();
        }
        
        // 3. 统计该总结的covered_until_message_id之后的消息数量
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getUserId, userId)
                        .eq(ConversationMessage::getAiCharacterId, aiCharacterId)
                        .gt(ConversationMessage::getId, latestSummary.getCoveredUntilMessageId())
        );
        
        return count.intValue();
    }
    
    /**
     * 参数校验
     */
    private void validateParameters(Long userId, Long aiCharacterId, String senderType, String content) {
        if (userId == null) {
            throw new SnorlaxClientException("userId不能为空");
        }
        
        if (aiCharacterId == null) {
            throw new SnorlaxClientException("aiCharacterId不能为空");
        }
        
        if (senderType == null || senderType.trim().isEmpty()) {
            throw new SnorlaxClientException("senderType不能为空");
        }
        
        if (!"USER".equals(senderType) && !"AI".equals(senderType)) {
            throw new SnorlaxClientException("senderType必须是USER或AI");
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new SnorlaxClientException("content不能为空");
        }
    }
}
