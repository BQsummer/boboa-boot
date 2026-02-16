package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.ScheduleRulePattern;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleRulePatternMapper {

    @Insert("""
            INSERT INTO schedule_rule_patterns(rule_id, weekday_mask, month_day, week_of_month, weekday)
            VALUES (#{ruleId}, #{weekdayMask}, #{monthDay}, #{weekOfMonth}, #{weekday})
            """)
    int insert(ScheduleRulePattern pattern);

    @Select("""
            <script>
            SELECT id, rule_id, weekday_mask, month_day, week_of_month, weekday, created_at, updated_at
            FROM schedule_rule_patterns
            WHERE rule_id IN
            <foreach collection='ruleIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    List<ScheduleRulePattern> findByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
