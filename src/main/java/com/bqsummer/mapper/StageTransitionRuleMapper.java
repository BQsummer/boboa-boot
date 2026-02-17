package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.StageTransitionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StageTransitionRuleMapper extends BaseMapper<StageTransitionRule> {

    @Select("SELECT * FROM stage_transition_rules WHERE from_stage_id = #{fromStageId} AND direction = #{direction} AND is_active = TRUE")
    List<StageTransitionRule> findActiveRulesByFromAndDirection(@Param("fromStageId") Integer fromStageId,
                                                                @Param("direction") String direction);
}
