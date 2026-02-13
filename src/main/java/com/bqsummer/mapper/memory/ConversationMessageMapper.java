package com.bqsummer.mapper.memory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.memory.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话消息Mapper接口
 * 
 * @author Boboa Boot Team
 * @date 2026-01-24
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
    
    /**
     * 查询最近的消息
     * 
     * @param userId 用户ID
     * @param aiCharacterId AI角色ID
     * @param limit 限制数量
     * @return 消息列表（按创建时间倒序）
     */
    List<ConversationMessage> findRecentMessages(
            @Param("userId") Long userId,
            @Param("aiCharacterId") Long aiCharacterId,
            @Param("limit") int limit
    );
}
