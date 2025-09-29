package com.bqsummer.service.im;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.Friend;
import com.bqsummer.common.dto.User;
import com.bqsummer.mapper.FriendMapper;
import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        // ensure users exist
        User u1 = userMapper.findById(userId);
        User u2 = userMapper.findById(friendId);
        if (u1 == null || u2 == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // insert both directions if not exists
        if (!exists(userId, friendId)) {
            Friend f = new Friend();
            f.setUserId(userId);
            f.setFriendUserId(friendId);
            friendMapper.insert(f);
        }
        if (!exists(friendId, userId)) {
            Friend r = new Friend();
            r.setUserId(friendId);
            r.setFriendUserId(userId);
            friendMapper.insert(r);
        }
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        QueryWrapper<Friend> qw1 = new QueryWrapper<>();
        qw1.eq("user_id", userId).eq("friend_user_id", friendId);
        friendMapper.delete(qw1);
        QueryWrapper<Friend> qw2 = new QueryWrapper<>();
        qw2.eq("user_id", friendId).eq("friend_user_id", userId);
        friendMapper.delete(qw2);
    }

    public List<User> listFriends(Long userId) {
        List<User> users = friendMapper.findFriendUsers(userId);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    private boolean exists(Long userId, Long friendId) {
        QueryWrapper<Friend> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("friend_user_id", friendId);
        return friendMapper.selectCount(qw) > 0;
    }
}


