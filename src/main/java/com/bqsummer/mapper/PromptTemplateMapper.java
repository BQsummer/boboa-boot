package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.prompt.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Prompt 模板 Mapper 接口
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    /**
     * 获取指定角色的最大版本号
     *
     * @param charId 角色ID
     * @return 最大版本号，如果不存在则返回 null
     */
    @Select("SELECT MAX(version) FROM prompt_template WHERE char_id = #{charId} AND is_deleted = FALSE")
    Integer getMaxVersionByCharId(@Param("charId") Long charId);

    /**
     * 将指定角色的所有模板标记为非最新版本
     *
     * @param charId 角色ID
     * @return 更新的行数
     */
    @Update("UPDATE prompt_template SET is_latest = FALSE WHERE char_id = #{charId} AND is_deleted = FALSE")
    int markAllAsNotLatest(@Param("charId") Long charId);

    /**
     * 统计角色下启用中的模板数量（排除指定模板ID）
     *
     * @param charId 角色ID
     * @param enabledStatus 启用状态码
     * @param excludeId 需要排除的模板ID
     * @return 启用模板数量
     */
    @Select("""
            SELECT COUNT(1)
            FROM prompt_template
            WHERE char_id = #{charId}
              AND status = #{enabledStatus}
              AND is_deleted = FALSE
              AND id <> #{excludeId}
            """)
    long countEnabledByCharIdExcludingId(@Param("charId") Long charId,
                                         @Param("enabledStatus") Integer enabledStatus,
                                         @Param("excludeId") Long excludeId);
}
