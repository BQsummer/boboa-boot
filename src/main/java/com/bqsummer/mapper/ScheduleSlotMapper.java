package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.ScheduleSlot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleSlotMapper {

    @Insert("""
            INSERT INTO schedule_slots(rule_id, start_time, end_time, location_text, activity_text, detail)
            VALUES (#{ruleId}, #{startTime}, #{endTime}, #{locationText}, #{activityText}, #{detail})
            """)
    int insert(ScheduleSlot slot);

    @Select("""
            <script>
            SELECT id, rule_id, start_time, end_time, location_text, activity_text, detail
            FROM schedule_slots
            WHERE rule_id IN
            <foreach collection='ruleIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<ScheduleSlot> findByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
