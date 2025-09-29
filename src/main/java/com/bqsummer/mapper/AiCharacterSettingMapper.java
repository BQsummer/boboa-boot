package com.bqsummer.mapper;

import com.bqsummer.common.dto.AiCharacterSetting;
import org.apache.ibatis.annotations.*;

/**
 * AI 人物用户个性化设置 Mapper
 */
@Mapper
public interface AiCharacterSettingMapper {

    @Select("SELECT * FROM ai_character_settings WHERE user_id = #{userId} AND character_id = #{characterId} AND is_deleted = 0")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "characterId", column = "character_id"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "memorialDay", column = "memorial_day"),
            @Result(property = "customParams", column = "custom_params"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time")
    })
    AiCharacterSetting findByUserAndCharacter(@Param("userId") Long userId, @Param("characterId") Long characterId);

    @Insert("INSERT INTO ai_character_settings (user_id, character_id, name, avatar_url, memorial_day, `relationship`, `background`, `language`, custom_params) " +
            "VALUES (#{userId}, #{characterId}, #{name}, #{avatarUrl}, #{memorialDay}, #{relationship}, #{background}, #{language}, #{customParams}) \n" +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), avatar_url = VALUES(avatar_url), memorial_day = VALUES(memorial_day), `relationship` = VALUES(`relationship`), `background` = VALUES(`background`), `language` = VALUES(`language`), custom_params = VALUES(custom_params), is_deleted = 0, updated_time = NOW()")
    int upsert(AiCharacterSetting setting);

    @Update({
            "<script>",
            "UPDATE ai_character_settings",
            "<set>",
            "<if test='name != null'>name = #{name},</if>",
            "<if test='avatarUrl != null'>avatar_url = #{avatarUrl},</if>",
            "<if test='memorialDay != null'>memorial_day = #{memorialDay},</if>",
            "<if test='relationship != null'>`relationship` = #{relationship},</if>",
            "<if test='background != null'>`background` = #{background},</if>",
            "<if test='language != null'>`language` = #{language},</if>",
            "<if test='customParams != null'>custom_params = #{customParams},</if>",
            "updated_time = NOW()",
            "</set>",
            "WHERE id = #{id} AND is_deleted = 0",
            "</script>"
    })
    int update(AiCharacterSetting setting);

    @Update("UPDATE ai_character_settings SET is_deleted = 1, updated_time = NOW() WHERE user_id = #{userId} AND character_id = #{characterId} AND is_deleted = 0")
    int softDelete(@Param("userId") Long userId, @Param("characterId") Long characterId);
}

