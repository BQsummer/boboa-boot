package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.prompt.PostProcessStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostProcessStepMapper extends BaseMapper<PostProcessStep> {

    @Delete("DELETE FROM post_process_step WHERE pipeline_id = #{pipelineId}")
    int deleteByPipelineId(@Param("pipelineId") Long pipelineId);
}
