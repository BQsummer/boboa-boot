package com.bqsummer.mapper;

import com.bqsummer.common.dto.RefreshToken;
import org.apache.ibatis.annotations.*;

/**
 * 刷新令牌数据访问接口
 */
@Mapper
public interface RefreshTokenMapper {

    @Select("SELECT * FROM refresh_tokens WHERE token = #{token} AND expires_at > NOW()")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "expiresAt", column = "expires_at"),
        @Result(property = "createdTime", column = "created_time")
    })
    RefreshToken findByToken(@Param("token") String token);

    @Insert("INSERT INTO refresh_tokens (user_id, token, expires_at) " +
            "VALUES (#{userId}, #{token}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefreshToken refreshToken);

    @Delete("DELETE FROM refresh_tokens WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM refresh_tokens WHERE token = #{token}")
    int deleteByToken(@Param("token") String token);

    @Delete("DELETE FROM refresh_tokens WHERE expires_at < NOW()")
    int deleteExpiredTokens();
}
