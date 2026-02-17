package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.StagePrompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StagePromptMapper extends BaseMapper<StagePrompt> {

    @Select("SELECT MAX(version) FROM stage_prompts WHERE stage_code = #{stageCode} AND prompt_type = #{promptType}")
    Integer getMaxVersion(@Param("stageCode") String stageCode, @Param("promptType") String promptType);
}
