package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.MonthlyPlan;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 虚拟人物月度计划 Mapper
 */
@Mapper
public interface MonthlyPlanMapper {

    @Select("SELECT * FROM monthly_plans WHERE id = #{id} AND is_deleted = 0")
    @Results(id = "monthlyPlanResultMap", value = {
            @Result(property = "characterId", column = "character_id"),
            @Result(property = "dayRule", column = "day_rule"),
            @Result(property = "startTime", column = "start_time"),
            @Result(property = "durationMin", column = "duration_min"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time")
    })
    MonthlyPlan findById(@Param("id") Long id);

    @Select("SELECT * FROM monthly_plans WHERE character_id = #{characterId} AND is_deleted = 0 ORDER BY start_time ASC")
    @ResultMap("monthlyPlanResultMap")
    List<MonthlyPlan> listByCharacterId(@Param("characterId") Long characterId);

    @Insert("INSERT INTO monthly_plans (character_id, day_rule, start_time, duration_min, location, action, participants, extra) " +
            "VALUES (#{characterId}, #{dayRule}, #{startTime}, #{durationMin}, #{location}, #{action}, #{participants}, #{extra})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MonthlyPlan plan);

    @Update({
            "<script>",
            "UPDATE monthly_plans",
            "<set>",
            "<if test='dayRule != null'>day_rule = #{dayRule},</if>",
            "<if test='startTime != null'>start_time = #{startTime},</if>",
            "<if test='durationMin != null'>duration_min = #{durationMin},</if>",
            "<if test='location != null'>location = #{location},</if>",
            "<if test='action != null'>action = #{action},</if>",
            "<if test='participants != null'>participants = #{participants},</if>",
            "<if test='extra != null'>extra = #{extra},</if>",
            "updated_time = NOW()",
            "</set>",
            "WHERE id = #{id} AND is_deleted = 0",
            "</script>"
    })
    int update(MonthlyPlan plan);

    @Update("UPDATE monthly_plans SET is_deleted = 1, updated_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);
}
