package com.bqsummer.mapper;

import com.bqsummer.common.dto.auth.Role;
import com.bqsummer.common.dto.auth.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper {

    @Select("SELECT u.*, r.role_name FROM users u " +
            "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN roles r ON ur.role_id = r.id " +
            "WHERE (u.username = #{username} OR u.email = #{username}) AND u.is_deleted = 0")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "email", column = "email"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "password", column = "password"),
        @Result(property = "nickName", column = "nick_name"),
        @Result(property = "avatar", column = "avatar"),
        @Result(property = "status", column = "status"),
        @Result(property = "isDeleted", column = "is_deleted"),
        @Result(property = "lastLoginTime", column = "last_login_time"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "roles", column = "id", many = @Many(select = "findRolesByUserId"))
    })
    User findByUsernameOrEmail(@Param("username") String username);

    @Select("SELECT r.* FROM roles r " +
            "INNER JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "roleName", column = "role_name"),
        @Result(property = "description", column = "description"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time")
    })
    List<Role> findRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM users WHERE id = #{id} AND is_deleted = 0")
    @Results({
        @Result(property = "nickName", column = "nick_name"),
        @Result(property = "isDeleted", column = "is_deleted"),
        @Result(property = "lastLoginTime", column = "last_login_time"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "userType", column = "user_type")
    })
    User findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    @Insert("INSERT INTO users (username, email, phone, password, nick_name, status, user_type) " +
            "VALUES (#{username}, #{email}, #{phone}, #{password}, #{nickName}, #{status}, #{userType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Insert("INSERT INTO user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT id FROM roles WHERE role_name = #{roleName}")
    Long findRoleIdByName(@Param("roleName") String roleName);

    @Update("UPDATE users SET last_login_time = #{loginTime} WHERE id = #{userId}")
    int updateLastLoginTime(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    @Update("UPDATE users SET is_deleted = 1, status = 0, updated_time = NOW() WHERE id = #{userId} AND is_deleted = 0")
    int softDelete(@Param("userId") Long userId);

    /**
     * 更新用户昵称
     */
    @Update("UPDATE users SET nick_name = #{nickName}, updated_time = NOW() WHERE id = #{userId}")
    int updateNickName(@Param("userId") Long userId, @Param("nickName") String nickName);

    /**
     * 更新用户头像
     */
    @Update("UPDATE users SET avatar = #{avatar}, updated_time = NOW() WHERE id = #{userId}")
    int updateAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 插入用户（支持userType字段）
     */
    @Insert("INSERT INTO users (username, email, phone, password, nick_name, avatar, status, user_type) " +
            "VALUES (#{username}, #{email}, #{phone}, #{password}, #{nickName}, #{avatar}, #{status}, #{userType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWithType(User user);

    /**
     * 搜索用户（按用户名或昵称模糊搜索，排除当前用户）
     */
    @Select("SELECT * FROM users WHERE (username LIKE '%' || #{keyword} || '%' OR nick_name LIKE '%' || #{keyword} || '%') " +
            "AND id != #{currentUserId} AND is_deleted = 0 AND status = 1 ORDER BY username LIMIT 20")
    @Results({
        @Result(property = "nickName", column = "nick_name"),
        @Result(property = "isDeleted", column = "is_deleted"),
        @Result(property = "lastLoginTime", column = "last_login_time"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "userType", column = "user_type")
    })
    List<User> searchUsers(@Param("keyword") String keyword, @Param("currentUserId") Long currentUserId);
}
