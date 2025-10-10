package com.bqsummer.service.im;

import com.bqsummer.common.dto.auth.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 */
@Service
public class FriendService {

    // 模拟数据存储
    private final Map<Long, User> userStore = new HashMap<>();
    private final Map<Long, List<Long>> friendRelations = new HashMap<>();

    public FriendService() {
        // 初始化测试数据
        initTestData();
    }

    private void initTestData() {
        // 创建测试用户
        User user1 = User.builder()
                .id(1L)
                .username("testuser1")
                .nickname("测试用户1")
                .email("test1@example.com")
                .avatar("avatar1.jpg")
                .phone("13800138001")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("testuser2")
                .nickname("测试用户2")
                .email("test2@example.com")
                .avatar("avatar2.jpg")
                .phone("13800138002")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        User user3 = User.builder()
                .id(3L)
                .username("testuser3")
                .nickname("测试用户3")
                .email("test3@example.com")
                .avatar("avatar3.jpg")
                .phone("13800138003")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userStore.put(1L, user1);
        userStore.put(2L, user2);
        userStore.put(3L, user3);

        // 初始化好友关系
        friendRelations.put(1L, new ArrayList<>());
        friendRelations.put(2L, new ArrayList<>());
        friendRelations.put(3L, new ArrayList<>());
    }

    /**
     * 添加好友
     */
    public void addFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        if (!userStore.containsKey(userId) || !userStore.containsKey(friendId)) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (isFriend(userId, friendId)) {
            throw new IllegalArgumentException("已经是好友关系");
        }

        friendRelations.get(userId).add(friendId);
        friendRelations.get(friendId).add(userId);
    }

    /**
     * 删除好友
     */
    public void removeFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (!isFriend(userId, friendId)) {
            throw new IllegalArgumentException("不是好友关系");
        }

        friendRelations.get(userId).remove(friendId);
        friendRelations.get(friendId).remove(userId);
    }

    /**
     * 获取好友列表
     */
    public List<User> listFriends(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        List<Long> friendIds = friendRelations.get(userId);
        if (friendIds == null) {
            return new ArrayList<>();
        }

        return friendIds.stream()
                .map(userStore::get)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否为好友关系
     */
    public boolean isFriend(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return false;
        }

        List<Long> friendIds = friendRelations.get(userId);
        return friendIds != null && friendIds.contains(friendId);
    }

    /**
     * 搜索用户
     */
    public List<User> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }

        return userStore.values().stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> user.getUsername().contains(keyword) ||
                              user.getNickname().contains(keyword))
                .collect(Collectors.toList());
    }

    // 测试辅助方法
    public User getUserById(Long userId) {
        return userStore.get(userId);
    }

    public void clearFriendRelations() {
        friendRelations.clear();
        friendRelations.put(1L, new ArrayList<>());
        friendRelations.put(2L, new ArrayList<>());
        friendRelations.put(3L, new ArrayList<>());
    }
}
