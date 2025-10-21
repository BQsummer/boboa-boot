package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.im.Friend;
import com.bqsummer.common.dto.auth.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    @Select("SELECT u.* FROM friends f JOIN users u ON f.friend_user_id = u.id WHERE f.user_id = #{userId} ORDER BY f.created_time DESC")
    @Results({
            @Result(property = "nickName", column = "nick_name"),
            @Result(property = "lastLoginTime", column = "last_login_time"),
            @Result(property = "createdTime", column = "created_time"),
            @Result(property = "updatedTime", column = "updated_time"),
            @Result(property = "userType", column = "user_type")
    })
    List<User> findFriendUsers(Long userId);
}

