package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.AiCharacterSetting;
import org.apache.ibatis.annotations.*;

/**
 * AI 人物个性化设置 Mapper
 */
@Mapper
public interface AiCharacterSettingMapper {

    @Select("SELECT * FROM ai_character_settings WHERE user_id = #{userId} AND character_id = #{characterId} AND is_deleted = FALSE")
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
            "ON CONFLICT (user_id, character_id) DO UPDATE SET",
            "name = EXCLUDED.name,",
            "avatar_url = EXCLUDED.avatar_url,",
            "memorial_day = EXCLUDED.memorial_day,",
            "relationship = EXCLUDED.relationship,",
            "background = EXCLUDED.background,",
            "language = EXCLUDED.language,",
            "custom_params = EXCLUDED.custom_params,",
            "is_deleted = EXCLUDED.is_deleted,",
            "updated_time = NOW()",
            "</script>"
    })
    int upsert(AiCharacterSetting setting);

    @Update("UPDATE ai_character_settings SET is_deleted = TRUE, updated_time = NOW() WHERE user_id = #{userId} AND character_id = #{characterId} AND is_deleted = FALSE")
    int softDelete(@Param("userId") Long userId, @Param("characterId") Long characterId);
}
