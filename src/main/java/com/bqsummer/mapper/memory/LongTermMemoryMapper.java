package com.bqsummer.mapper.memory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.memory.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 长期记忆Mapper
 */
@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    /**
     * 根据记忆类型查询记忆
     *
     * @param userId        用户ID
     * @param aiCharacterId AI角色ID
     * @param memoryType    记忆类型
     * @param limit         返回数量
     * @return 记忆列表（按重要性降序）
     */
    List<LongTermMemory> findByType(@Param("userId") Long userId,
                                     @Param("aiCharacterId") Long aiCharacterId,
                                     @Param("memoryType") String memoryType,
                                     @Param("limit") int limit);

    /**
     * 查询高重要性记忆
     *
     * @param userId             用户ID
     * @param aiCharacterId      AI角色ID
     * @param minImportance      最小重要性阈值
     * @param limit              返回数量
     * @return 记忆列表（按重要性降序）
     */
    List<LongTermMemory> findHighImportance(@Param("userId") Long userId,
                                             @Param("aiCharacterId") Long aiCharacterId,
                                             @Param("minImportance") float minImportance,
                                             @Param("limit") int limit);

    /**
     * 向量相似度搜索
     *
     * @param userId          用户ID
     * @param aiCharacterId   AI角色ID
     * @param queryEmbedding  查询向量（1536维）
     * @param topK            返回数量
     * @return 记忆列表（按相似度降序）
     */
    List<LongTermMemory> searchByEmbedding(@Param("userId") Long userId,
                                            @Param("aiCharacterId") Long aiCharacterId,
                                            @Param("queryEmbedding") float[] queryEmbedding,
                                            @Param("topK") int topK);

    /**
     * 更新访问时间
     *
     * @param memoryId 记忆ID
     * @param now      当前时间
     * @return 更新行数
     */
    int updateAccessTime(@Param("memoryId") Long memoryId,
                         @Param("now") LocalDateTime now);
}
