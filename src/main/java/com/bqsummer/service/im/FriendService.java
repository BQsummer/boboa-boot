package com.bqsummer.service.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.im.Friend;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.FriendMapper;
import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友关系业务逻辑
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        validateUserIds(userId, friendId);

        ensureUserExists(userId);
        ensureUserExists(friendId);

        if (isFriend(userId, friendId)) {
            throw new SnorlaxClientException(409, "已经是好友关系");
        }

        LocalDateTime now = LocalDateTime.now();
        Friend forward = new Friend();
        forward.setUserId(userId);
        forward.setFriendUserId(friendId);
        forward.setCreatedTime(now);

        Friend reverse = new Friend();
        reverse.setUserId(friendId);
        reverse.setFriendUserId(userId);
        reverse.setCreatedTime(now);

        try {
            friendMapper.insert(forward);
            friendMapper.insert(reverse);
        } catch (DuplicateKeyException ex) {
            // 并发场景下的幂等保护，事务会自动回滚
            throw new SnorlaxClientException(409, "已经是好友关系");
        }
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        validateUserIds(userId, friendId);

        if (!isFriend(userId, friendId)) {
            throw new SnorlaxClientException(400, "不是好友关系");
        }

        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, friendId));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendUserId, userId));
    }

    public List<User> listFriends(Long userId) {
        if (userId == null) {
            throw new SnorlaxClientException(400, "用户ID不能为空");
        }
        List<User> users = friendMapper.findFriendUsers(userId);
        return users == null ? Collections.emptyList() : users;
    }

    public boolean isFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null || Objects.equals(userId, friendId)) {
            return false;
        }
    Long count = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, friendId));
    return count != null && count > 0;
    }

    public List<User> searchUsers(String keyword, Long currentUserId) {
        if (currentUserId == null) {
            throw new SnorlaxClientException(400, "当前用户ID不能为空");
        }
        if (!StringUtils.hasText(keyword)) {
            throw new SnorlaxClientException(400, "搜索关键词不能为空");
        }
        String trimmed = keyword.trim();
        List<User> candidates = userMapper.searchUsers(trimmed, currentUserId);
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
    Set<Long> friendIds = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
            .eq(Friend::getUserId, currentUserId))
                .stream()
                .map(Friend::getFriendUserId)
                .collect(Collectors.toSet());
        if (friendIds.isEmpty()) {
            return candidates;
        }
        return candidates.stream()
                .filter(user -> user.getId() != null && !friendIds.contains(user.getId()))
                .collect(Collectors.toList());
    }

    private void validateUserIds(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new SnorlaxClientException(400, "用户ID不能为空");
        }
        if (Objects.equals(userId, friendId)) {
            throw new SnorlaxClientException(400, "不能添加自己为好友");
        }
    }

    private void ensureUserExists(Long userId) {
        if (userMapper.findById(userId) == null) {
            throw new SnorlaxClientException(404, "用户不存在");
        }
    }
}
