package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.relationship.UserRelationshipState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRelationshipStateMapper extends BaseMapper<UserRelationshipState> {

    @Select("SELECT * FROM user_relationship_state WHERE user_id = #{userId} AND ai_character_id = #{aiCharacterId} LIMIT 1")
    UserRelationshipState findByUserAndCharacter(@Param("userId") Long userId, @Param("aiCharacterId") Long aiCharacterId);
}
