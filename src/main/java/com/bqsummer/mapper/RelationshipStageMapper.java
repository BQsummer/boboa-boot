package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.RelationshipStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RelationshipStageMapper extends BaseMapper<RelationshipStage> {

    @Select("SELECT * FROM relationship_stages WHERE code = #{code} LIMIT 1")
    RelationshipStage findByCode(@Param("code") String code);

    @Select("SELECT * FROM relationship_stages WHERE is_active = TRUE ORDER BY level ASC, id ASC LIMIT 1")
    RelationshipStage findFirstActiveStage();
}
