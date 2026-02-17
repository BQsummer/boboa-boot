package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.InteractionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface InteractionLogMapper extends BaseMapper<InteractionLog> {

    @Select("SELECT COALESCE(SUM(points_applied), 0) FROM interaction_log WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND points_applied > 0 AND created_at >= #{startAt} AND created_at < #{endAt}")
    Integer sumPositiveAppliedPointsInRange(@Param("userId") Long userId,
                                            @Param("aiCharacterId") Long aiCharacterId,
                                            @Param("startAt") LocalDateTime startAt,
                                            @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(1) FROM interaction_log WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND signal_type = #{signalType} AND window_key = #{windowKey}")
    Long countByWindowKey(@Param("userId") Long userId,
                          @Param("aiCharacterId") Long aiCharacterId,
                          @Param("signalType") String signalType,
                          @Param("windowKey") String windowKey);

    @Select("SELECT COUNT(1) FROM interaction_log WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND signal_type = #{signalType} AND points_applied <> 0")
    Long countAppliedBySignal(@Param("userId") Long userId,
                              @Param("aiCharacterId") Long aiCharacterId,
                              @Param("signalType") String signalType);

    @Select("SELECT MAX(created_at) FROM interaction_log WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND signal_type <> #{signalType}")
    LocalDateTime findLastInteractionAtExcludingSignal(@Param("userId") Long userId,
                                                        @Param("aiCharacterId") Long aiCharacterId,
                                                        @Param("signalType") String signalType);

    @Select("SELECT COALESCE(SUM(ABS(points_applied)), 0) FROM interaction_log WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} AND signal_type = #{signalType} AND created_at > #{since} AND points_applied < 0")
    Integer sumAbsNegativePointsBySignalSince(@Param("userId") Long userId,
                                               @Param("aiCharacterId") Long aiCharacterId,
                                               @Param("signalType") String signalType,
                                               @Param("since") LocalDateTime since);
}
