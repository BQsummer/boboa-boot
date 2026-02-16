package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.ScheduleRule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface ScheduleRuleMapper {

    @Insert("""
            INSERT INTO schedule_rules(character_key, title, recurrence_type, interval, priority, is_active, valid_from, valid_to)
            VALUES (#{characterKey}, #{title}, #{recurrenceType}, #{interval}, #{priority}, #{isActive}, #{validFrom}, #{validTo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleRule rule);

    @Select("""
            SELECT id, character_key, title, recurrence_type, interval, priority, is_active, valid_from, valid_to
            FROM schedule_rules
            WHERE character_key = #{characterKey} AND is_active = TRUE
            """)
    List<ScheduleRule> findActiveByCharacterKey(@Param("characterKey") String characterKey);

    @Select("""
            SELECT id, character_key, title, recurrence_type, interval, priority, is_active, valid_from, valid_to
            FROM schedule_rules
            WHERE character_key = #{characterKey}
            ORDER BY priority DESC, id DESC
            """)
    List<ScheduleRule> findByCharacterKey(@Param("characterKey") String characterKey);

    @Delete("DELETE FROM schedule_rules WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
