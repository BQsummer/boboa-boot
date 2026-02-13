package com.bqsummer.mapper.memory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.memory.ConversationSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话总结Mapper
 */
@Mapper
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummary> {

    /**
     * 获取最新的总结
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param limit         返回数量
     * @return 总结列表（按创建时间降序）
     */
    List<ConversationSummary> findLatestSummaries(@Param("userId") Long userId,
                                                   @Param("aiCharacterId") Long aiCharacterId,
                                                   @Param("limit") int limit);

    /**
     * 获取最新一条总结
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @return 最新总结，如果没有则返回null
     */
    ConversationSummary findLatestSummary(@Param("userId") Long userId,
                                          @Param("aiCharacterId") Long aiCharacterId);
}
