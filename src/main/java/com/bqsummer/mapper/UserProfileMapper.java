package com.bqsummer.mapper;

import com.bqsummer.common.dto.auth.UserProfile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserProfileMapper {

    @Select("SELECT id, user_id, gender, birthday, height_cm, mbti, occupation, interests, photos, created_time, updated_time " +
            "FROM user_profiles WHERE user_id = #{userId} LIMIT 1")
    UserProfile selectByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_profiles (user_id, gender, birthday, height_cm, mbti, occupation, interests, photos) " +
            "VALUES (#{userId}, #{gender}, #{birthday}, #{heightCm}, #{mbti}, #{occupation}, #{interests}, #{photos}) " +
            "ON DUPLICATE KEY UPDATE " +
            "gender = VALUES(gender), " +
            "birthday = VALUES(birthday), " +
            "height_cm = VALUES(height_cm), " +
            "mbti = VALUES(mbti), " +
            "occupation = VALUES(occupation), " +
            "interests = VALUES(interests), " +
            "photos = VALUES(photos), " +
            "updated_time = CURRENT_TIMESTAMP")
    int upsert(UserProfile profile);
}

