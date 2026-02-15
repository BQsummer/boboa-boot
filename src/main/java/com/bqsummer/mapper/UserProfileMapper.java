package com.bqsummer.mapper;

import com.bqsummer.common.dto.auth.UserProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserProfileMapper {

    @Select("SELECT id, user_id, gender, birthday, height_cm, mbti, occupation, interests, photos, \"desc\", created_time, updated_time " +
            "FROM user_profiles WHERE user_id = #{userId} LIMIT 1")
    UserProfile selectByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_profiles (user_id, gender, birthday, height_cm, mbti, occupation, interests, photos, \"desc\") " +
            "VALUES (#{userId}, #{gender}, #{birthday}, #{heightCm}, #{mbti}, #{occupation}, #{interests}, #{photos}, #{desc}) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "gender = EXCLUDED.gender, " +
            "birthday = EXCLUDED.birthday, " +
            "height_cm = EXCLUDED.height_cm, " +
            "mbti = EXCLUDED.mbti, " +
            "occupation = EXCLUDED.occupation, " +
            "interests = EXCLUDED.interests, " +
            "photos = EXCLUDED.photos, " +
            "\"desc\" = EXCLUDED.\"desc\", " +
            "updated_time = CURRENT_TIMESTAMP")
    int upsert(UserProfile profile);

    @Delete("DELETE FROM user_profiles WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}

