package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.AiCharacterSetting;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
            @Result(property = "emotion", column = "emotion"),
            @Result(property = "customParams", column = "custom_params"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time")
    })
    AiCharacterSetting findByUserAndCharacter(@Param("userId") Long userId, @Param("characterId") Long characterId);

    @Select("SELECT * FROM ai_character_settings WHERE user_id IS NULL AND character_id = #{characterId} AND is_deleted = FALSE LIMIT 1")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "characterId", column = "character_id"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "memorialDay", column = "memorial_day"),
            @Result(property = "emotion", column = "emotion"),
            @Result(property = "customParams", column = "custom_params"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time")
    })
    AiCharacterSetting findDefaultByCharacter(@Param("characterId") Long characterId);

    @Select("SELECT * FROM ai_character_settings WHERE character_id = #{characterId} AND is_deleted = FALSE ORDER BY updated_time DESC")
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "characterId", column = "character_id"),
            @Result(property = "avatarUrl", column = "avatar_url"),
            @Result(property = "memorialDay", column = "memorial_day"),
            @Result(property = "emotion", column = "emotion"),
            @Result(property = "customParams", column = "custom_params"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time")
    })
    List<AiCharacterSetting> listByCharacter(@Param("characterId") Long characterId);

    @Update({
            "<script>",
            "UPDATE ai_character_settings",
            "SET name = #{name},",
            "avatar_url = #{avatarUrl},",
            "memorial_day = #{memorialDay},",
            "relationship = #{relationship},",
            "emotion = #{emotion},",
            "background = #{background},",
            "language = #{language},",
            "custom_params = #{customParams},",
            "is_deleted = #{isDeleted},",
            "updated_time = NOW()",
            "WHERE user_id IS NULL AND character_id = #{characterId}",
            "</script>"
    })
    int updateDefault(AiCharacterSetting setting);

    @Insert({
            "<script>",
            "INSERT INTO ai_character_settings (user_id, character_id, name, avatar_url, memorial_day, relationship, emotion, background, language, custom_params, is_deleted, created_time, updated_time)",
            "VALUES (NULL, #{characterId}, #{name}, #{avatarUrl}, #{memorialDay}, #{relationship}, #{emotion}, #{background}, #{language}, #{customParams}, #{isDeleted}, NOW(), NOW())",
            "</script>"
    })
    int insertDefault(AiCharacterSetting setting);

    @Insert({
            "<script>",
            "INSERT INTO ai_character_settings (user_id, character_id, name, avatar_url, memorial_day, relationship, emotion, background, language, custom_params, is_deleted, created_time, updated_time)",
            "VALUES (#{userId}, #{characterId}, #{name}, #{avatarUrl}, #{memorialDay}, #{relationship}, #{emotion}, #{background}, #{language}, #{customParams}, #{isDeleted}, NOW(), NOW())",
            "ON CONFLICT (user_id, character_id) DO UPDATE SET",
            "name = EXCLUDED.name,",
            "avatar_url = EXCLUDED.avatar_url,",
            "memorial_day = EXCLUDED.memorial_day,",
            "relationship = EXCLUDED.relationship,",
            "emotion = EXCLUDED.emotion,",
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

    @Update("UPDATE ai_character_settings SET is_deleted = TRUE, updated_time = NOW() WHERE user_id IS NULL AND character_id = #{characterId} AND is_deleted = FALSE")
    int softDeleteDefault(@Param("characterId") Long characterId);
}
