package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.config.Config;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {

    @Select("select value, type from config where env = #{env} and name = #{name} and application = #{application} and status = 'ACTIVE' limit 1")
    Config getConfigFromDB(@Param("env") String env, @Param("application") String application, @Param("name") String name);

    @Select("select id, value from config where env = #{env} and name = #{name} and application = #{application} limit 1")
    Config findConfigIdAndValue(@Param("env") String env, @Param("application") String application, @Param("name") String name);

    @Insert("""
            insert into config (env, application, name, value, type, "sensitive", status, catalog, created_at, updated_at, created_by, updated_by)
            values (#{env}, #{application}, #{name}, #{value}, #{type}, #{sensitive}, #{status}, #{catalog}, #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})
            """)
    int insertConfigForPostgres(@Param("env") String env,
                                @Param("application") String application,
                                @Param("name") String name,
                                @Param("value") String value,
                                @Param("type") String type,
                                @Param("sensitive") String sensitive,
                                @Param("status") String status,
                                @Param("catalog") String catalog,
                                @Param("createdAt") LocalDateTime createdAt,
                                @Param("updatedAt") LocalDateTime updatedAt,
                                @Param("createdBy") String createdBy,
                                @Param("updatedBy") String updatedBy);

    @Update("update config set value = #{value}, updated_at = #{updatedAt}, updated_by = #{updatedBy} where id = #{id}")
    int updateConfigValueById(@Param("id") Long id,
                              @Param("value") String value,
                              @Param("updatedAt") LocalDateTime updatedAt,
                              @Param("updatedBy") String updatedBy);
}
