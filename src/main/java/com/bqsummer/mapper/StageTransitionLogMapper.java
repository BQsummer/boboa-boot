package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.StageTransitionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StageTransitionLogMapper extends BaseMapper<StageTransitionLog> {

    @Select("SELECT * FROM stage_transition_logs WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} ORDER BY created_at DESC LIMIT #{limit}")
    List<StageTransitionLog> findRecentByUserAndCharacter(@Param("userId") Long userId,
                                                          @Param("aiCharacterId") Long aiCharacterId,
                                                          @Param("limit") Integer limit);
}
