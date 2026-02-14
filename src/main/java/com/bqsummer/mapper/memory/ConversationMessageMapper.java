package com.bqsummer.mapper.memory;

import com.bqsummer.common.dto.memory.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话消息查询 Mapper（数据来源: message 表）
 */
@Mapper
public interface ConversationMessageMapper {

    List<ConversationMessage> findRecentMessages(
            @Param("userId") Long userId,
            @Param("aiCharacterId") Long aiCharacterId,
            @Param("limit") int limit
    );

    Long countMessages(
            @Param("userId") Long userId,
            @Param("aiCharacterId") Long aiCharacterId
    );

    Long countMessagesAfterId(
            @Param("userId") Long userId,
            @Param("aiCharacterId") Long aiCharacterId,
            @Param("startMessageId") Long startMessageId
    );

    List<ConversationMessage> findMessagesForSummary(
            @Param("userId") Long userId,
            @Param("aiCharacterId") Long aiCharacterId,
            @Param("startMessageId") Long startMessageId
    );
}
