package com.bqsummer.mapper;

import com.bqsummer.common.dto.character.AiCharacter;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI 人物模板 Mapper
 */
@Mapper
public interface AiCharacterMapper {

    @Select("SELECT * FROM ai_characters WHERE id = #{id} AND is_deleted = FALSE")
    @Results({
            @Result(property = "createdByUserId", column = "created_by_user_id"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time"),
            @Result(property = "associatedUserId", column = "associated_user_id")
    })
    AiCharacter findById(@Param("id") Long id);

    @Select("SELECT * FROM ai_characters WHERE associated_user_id = #{associatedUserId} AND is_deleted = FALSE LIMIT 1")
    @Results({
            @Result(property = "createdByUserId", column = "created_by_user_id"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time"),
            @Result(property = "associatedUserId", column = "associated_user_id")
    })
    AiCharacter findByAssociatedUserId(@Param("associatedUserId") Long associatedUserId);

    @Select("SELECT * FROM ai_characters WHERE is_deleted = FALSE AND (visibility = 'PUBLIC' OR created_by_user_id = #{userId}) ORDER BY id DESC")
    @Results({
            @Result(property = "createdByUserId", column = "created_by_user_id"),
            @Result(property = "imageUrl", column = "image_url"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time"),
            @Result(property = "associatedUserId", column = "associated_user_id")
    })
    List<AiCharacter> listVisibleForUser(@Param("userId") Long userId);

    @Insert("INSERT INTO ai_characters (name, image_url, author, created_by_user_id, visibility, status) VALUES (#{name}, #{imageUrl}, #{author}, #{createdByUserId}, #{visibility}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiCharacter c);

    @Update({
            "<script>",
            "UPDATE ai_characters",
            "<set>",
            "<if test='name != null'>name = #{name},</if>",
            "<if test='imageUrl != null'>image_url = #{imageUrl},</if>",
            "<if test='author != null'>author = #{author},</if>",
            "<if test='visibility != null'>visibility = #{visibility},</if>",
            "<if test='status != null'>status = #{status},</if>",
            "updated_time = NOW()",
            "</set>",
            "WHERE id = #{id} AND is_deleted = FALSE",
            "</script>"
    })
    int update(AiCharacter c);

    @Update("UPDATE ai_characters SET is_deleted = TRUE, status = FALSE, updated_time = NOW() WHERE id = #{id} AND is_deleted = FALSE")
    int softDelete(@Param("id") Long id);

    /**
     * 更新AI角色的关联用户ID
     */
    @Update("UPDATE ai_characters SET associated_user_id = #{associatedUserId}, updated_time = NOW() WHERE id = #{id}")
    int updateAssociatedUserId(@Param("id") Long id, @Param("associatedUserId") Long associatedUserId);
}

