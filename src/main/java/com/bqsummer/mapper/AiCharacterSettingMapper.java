package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.AiCharacterSetting;
import org.apache.ibatis.annotations.*;

/**
 * AI 人物个性化设置 Mapper
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

    @Insert({
            "<script>",
            "INSERT INTO ai_character_settings (user_id, character_id, name, avatar_url, memorial_day, relationship, background, language, custom_params, is_deleted, created_time, updated_time)",
            "VALUES (#{userId}, #{characterId}, #{name}, #{avatarUrl}, #{memorialDay}, #{relationship}, #{background}, #{language}, #{customParams}, #{isDeleted}, NOW(), NOW())",
            "ON DUPLICATE KEY UPDATE",
            "name = VALUES(name),",
            "avatar_url = VALUES(avatar_url),",
            "memorial_day = VALUES(memorial_day),",
            "relationship = VALUES(relationship),",
            "background = VALUES(background),",
            "language = VALUES(language),",
            "custom_params = VALUES(custom_params),",
            "is_deleted = VALUES(is_deleted),",
            "updated_time = NOW()",
            "</script>"
    })
    int upsert(AiCharacterSetting setting);

    @Update("UPDATE ai_character_settings SET is_deleted = 1, updated_time = NOW() WHERE user_id = #{userId} AND character_id = #{characterId} AND is_deleted = 0")
    int softDelete(@Param("userId") Long userId, @Param("characterId") Long characterId);
}
