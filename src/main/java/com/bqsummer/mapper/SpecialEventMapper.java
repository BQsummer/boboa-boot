package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.SpecialEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface SpecialEventMapper {

    @Insert("""
            INSERT INTO special_events(character_key, title, start_at, end_at, location_text, activity_text, override_mode, priority, detail)
            VALUES (#{characterKey}, #{title}, #{startAt}, #{endAt}, #{locationText}, #{activityText}, #{overrideMode}, #{priority}, #{detail})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SpecialEvent event);

    @Select("""
            SELECT id, character_key, title, start_at, end_at, location_text, activity_text, override_mode, priority, detail, created_at, updated_at
            FROM special_events
            WHERE character_key = #{characterKey}
              AND start_at <= #{at}
              AND end_at > #{at}
            ORDER BY priority DESC, start_at DESC, id DESC
            LIMIT 1
            """)
    SpecialEvent findTopActiveEvent(@Param("characterKey") String characterKey, @Param("at") OffsetDateTime at);

    @Select("""
            SELECT id, character_key, title, start_at, end_at, location_text, activity_text, override_mode, priority, detail, created_at, updated_at
            FROM special_events
            WHERE character_key = #{characterKey}
            ORDER BY start_at DESC, priority DESC, id DESC
            """)
    List<SpecialEvent> findByCharacterKey(@Param("characterKey") String characterKey);

    @Delete("DELETE FROM special_events WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
