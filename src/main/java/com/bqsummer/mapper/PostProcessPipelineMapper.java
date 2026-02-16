package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.prompt.PostProcessPipeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostProcessPipelineMapper extends BaseMapper<PostProcessPipeline> {

    @Select("SELECT MAX(version) FROM post_process_pipeline WHERE name = #{name} AND is_deleted = FALSE")
    Integer getMaxVersionByName(@Param("name") String name);

    @Update("UPDATE post_process_pipeline SET is_latest = FALSE WHERE name = #{name} AND is_deleted = FALSE")
    int markAllAsNotLatest(@Param("name") String name);
}
