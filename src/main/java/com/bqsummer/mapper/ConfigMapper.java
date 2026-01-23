package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.config.Config;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {

    @Select("select value, type from config where env = #{env} and name = #{name} and application = #{application} and status = 'ACTIVE' limit 1")
    Config getConfigFromDB(@Param("env") String env, @Param("application") String application, @Param("name") String name);
}
