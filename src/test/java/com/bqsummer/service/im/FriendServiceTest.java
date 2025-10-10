package com.bqsummer.service.im;

import com.bqsummer.common.dto.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FriendService 单元测试
 * 覆盖所有业务逻辑分支
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("好友服务测试")
class FriendServiceTest {

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendService = new FriendService();
        // 重置好友关系，确保每个测试独立
        friendService.clearFriendRelations();
    }

    @Nested
    @DisplayName("添加好友测试")
    class AddFriendTests {

        @Test
        @DisplayName("成功添加好友")
        void shouldAddFriendSuccessfully() {
            // When
            friendService.addFriend(1L, 2L);

            // Then
            assertTrue(friendService.isFriend(1L, 2L));
            assertTrue(friendService.isFriend(2L, 1L)); // 双向关系
        }

        @Test
        @DisplayName("用户ID为null时抛出异常")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(null, 2L)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("好友ID为null时抛出异常")
        void shouldThrowExceptionWhenFriendIdIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(1L, null)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("用户ID和好友ID相同时抛出异常")
        void shouldThrowExceptionWhenAddingSelf() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(1L, 1L)
            );
            assertEquals("不能添加自己为好友", exception.getMessage());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void shouldThrowExceptionWhenUserNotExists() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(99L, 2L)
            );
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("好友不存在时抛出异常")
        void shouldThrowExceptionWhenFriendNotExists() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(1L, 99L)
            );
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("重复添加好友时抛出异常")
        void shouldThrowExceptionWhenAlreadyFriends() {
            // Given
            friendService.addFriend(1L, 2L);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(1L, 2L)
            );
            assertEquals("已经是好友关系", exception.getMessage());
        }

        @Test
        @DisplayName("反向重复添加好友时抛出异常")
        void shouldThrowExceptionWhenAlreadyFriendsReverse() {
            // Given
            friendService.addFriend(1L, 2L);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.addFriend(2L, 1L)
            );
            assertEquals("已经是好友关系", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("删除好友测试")
    class RemoveFriendTests {

        @Test
        @DisplayName("成功删除好友")
        void shouldRemoveFriendSuccessfully() {
            // Given
            friendService.addFriend(1L, 2L);
            assertTrue(friendService.isFriend(1L, 2L));

            // When
            friendService.removeFriend(1L, 2L);

            // Then
            assertFalse(friendService.isFriend(1L, 2L));
            assertFalse(friendService.isFriend(2L, 1L));
        }

        @Test
        @DisplayName("用户ID为null时抛出异常")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.removeFriend(null, 2L)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("好友ID为null时抛出异常")
        void shouldThrowExceptionWhenFriendIdIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.removeFriend(1L, null)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("删除不存在的好友关系时抛出异常")
        void shouldThrowExceptionWhenNotFriends() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.removeFriend(1L, 2L)
            );
            assertEquals("不是好友关系", exception.getMessage());
        }

        @Test
        @DisplayName("反向删除好友")
        void shouldRemoveFriendFromReverseSide() {
            // Given
            friendService.addFriend(1L, 2L);

            // When
            friendService.removeFriend(2L, 1L);

            // Then
            assertFalse(friendService.isFriend(1L, 2L));
            assertFalse(friendService.isFriend(2L, 1L));
        }
    }

    @Nested
    @DisplayName("获取好友列表测试")
    class ListFriendsTests {

        @Test
        @DisplayName("成功获取好友列表")
        void shouldListFriendsSuccessfully() {
            // Given
            friendService.addFriend(1L, 2L);
            friendService.addFriend(1L, 3L);

            // When
            List<User> friends = friendService.listFriends(1L);

            // Then
            assertEquals(2, friends.size());
            assertTrue(friends.stream().anyMatch(user -> user.getId().equals(2L)));
            assertTrue(friends.stream().anyMatch(user -> user.getId().equals(3L)));
        }

        @Test
        @DisplayName("获取空的好友列表")
        void shouldReturnEmptyListWhenNoFriends() {
            // When
            List<User> friends = friendService.listFriends(1L);

            // Then
            assertTrue(friends.isEmpty());
        }

        @Test
        @DisplayName("用户ID为null时抛出异常")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.listFriends(null)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("获取单个好友的列表")
        void shouldListSingleFriend() {
            // Given
            friendService.addFriend(1L, 2L);

            // When
            List<User> friends = friendService.listFriends(1L);

            // Then
            assertEquals(1, friends.size());
            assertEquals(2L, friends.get(0).getId());
            assertEquals("testuser2", friends.get(0).getUsername());
        }

        @Test
        @DisplayName("验证好友列表中用户信息完整性")
        void shouldReturnCompleteUserInformation() {
            // Given
            friendService.addFriend(1L, 2L);

            // When
            List<User> friends = friendService.listFriends(1L);

            // Then
            assertEquals(1, friends.size());
            User friend = friends.get(0);
            assertNotNull(friend.getId());
            assertNotNull(friend.getUsername());
            assertNotNull(friend.getNickname());
            assertNotNull(friend.getEmail());
            assertNotNull(friend.getCreateTime());
        }
    }

    @Nested
    @DisplayName("检查好友关系测试")
    class IsFriendTests {

        @Test
        @DisplayName("存在好友关系时返回true")
        void shouldReturnTrueForExistingFriendship() {
            // Given
            friendService.addFriend(1L, 2L);

            // When & Then
            assertTrue(friendService.isFriend(1L, 2L));
            assertTrue(friendService.isFriend(2L, 1L));
        }

        @Test
        @DisplayName("不存在好友关系时返回false")
        void shouldReturnFalseForNonExistingFriendship() {
            // When & Then
            assertFalse(friendService.isFriend(1L, 2L));
        }

        @Test
        @DisplayName("用户ID为null时返回false")
        void shouldReturnFalseWhenUserIdIsNull() {
            // When & Then
            assertFalse(friendService.isFriend(null, 2L));
        }

        @Test
        @DisplayName("好友ID为null时返回false")
        void shouldReturnFalseWhenFriendIdIsNull() {
            // When & Then
            assertFalse(friendService.isFriend(1L, null));
        }

        @Test
        @DisplayName("两个ID都为null时返回false")
        void shouldReturnFalseWhenBothIdsAreNull() {
            // When & Then
            assertFalse(friendService.isFriend(null, null));
        }

        @Test
        @DisplayName("检查自己与自己的关系")
        void shouldReturnFalseForSelfCheck() {
            // When & Then
            assertFalse(friendService.isFriend(1L, 1L));
        }
    }

    @Nested
    @DisplayName("搜索用户测试")
    class SearchUsersTests {

        @Test
        @DisplayName("按用户名搜索用户")
        void shouldSearchUsersByUsername() {
            // When
            List<User> results = friendService.searchUsers("testuser2", 1L);

            // Then
            assertEquals(1, results.size());
            assertEquals(2L, results.get(0).getId());
            assertEquals("testuser2", results.get(0).getUsername());
        }

        @Test
        @DisplayName("按昵称搜索用户")
        void shouldSearchUsersByNickname() {
            // When
            List<User> results = friendService.searchUsers("测试用户2", 1L);

            // Then
            assertEquals(1, results.size());
            assertEquals(2L, results.get(0).getId());
            assertEquals("测试用户2", results.get(0).getNickname());
        }

        @Test
        @DisplayName("搜索关键词匹配多个用户")
        void shouldReturnMultipleMatchingUsers() {
            // When
            List<User> results = friendService.searchUsers("testuser", 1L);

            // Then
            assertEquals(2, results.size()); // testuser2 and testuser3
            assertTrue(results.stream().anyMatch(user -> user.getId().equals(2L)));
            assertTrue(results.stream().anyMatch(user -> user.getId().equals(3L)));
        }

        @Test
        @DisplayName("搜索结果不包含当前用户")
        void shouldExcludeCurrentUserFromResults() {
            // When
            List<User> results = friendService.searchUsers("testuser1", 1L);

            // Then
            assertTrue(results.isEmpty()); // testuser1是当前用户，应该被排除
        }

        @Test
        @DisplayName("搜索无匹配结果")
        void shouldReturnEmptyListWhenNoMatches() {
            // When
            List<User> results = friendService.searchUsers("nonexistent", 1L);

            // Then
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("搜索关键词为null时抛出异常")
        void shouldThrowExceptionWhenKeywordIsNull() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.searchUsers(null, 1L)
            );
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("搜索关键词为空字符串时抛出异常")
        void shouldThrowExceptionWhenKeywordIsEmpty() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.searchUsers("", 1L)
            );
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("搜索关键词为空白字符时抛出异常")
        void shouldThrowExceptionWhenKeywordIsBlank() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> friendService.searchUsers("   ", 1L)
            );
            assertEquals("搜索关键词不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("部分匹配搜索")
        void shouldSupportPartialMatch() {
            // When
            List<User> results = friendService.searchUsers("user", 1L);

            // Then
            assertEquals(2, results.size()); // testuser2 and testuser3 contain "user"
        }

        @Test
        @DisplayName("大小写敏感搜索")
        void shouldBeCaseSensitive() {
            // When
            List<User> results = friendService.searchUsers("TestUser2", 1L);

            // Then
            assertTrue(results.isEmpty()); // 大小写不匹配
        }

        @Test
        @DisplayName("搜索中文昵称")
        void shouldSearchChineseNickname() {
            // When
            List<User> results = friendService.searchUsers("测试", 1L);

            // Then
            assertEquals(2, results.size()); // 测试用户2 and 测试用户3
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("获取不存在用户的信息")
        void shouldReturnNullForNonExistentUser() {
            // When
            User user = friendService.getUserById(99L);

            // Then
            assertNull(user);
        }

        @Test
        @DisplayName("添加好友后再删除，然后重新添加")
        void shouldAllowReAddingAfterRemoval() {
            // Given
            friendService.addFriend(1L, 2L);
            friendService.removeFriend(1L, 2L);

            // When
            friendService.addFriend(1L, 2L);

            // Then
            assertTrue(friendService.isFriend(1L, 2L));
        }

        @Test
        @DisplayName("同时添加多个好友")
        void shouldAddMultipleFriends() {
            // When
            friendService.addFriend(1L, 2L);
            friendService.addFriend(1L, 3L);

            // Then
            assertTrue(friendService.isFriend(1L, 2L));
            assertTrue(friendService.isFriend(1L, 3L));
            assertFalse(friendService.isFriend(2L, 3L)); // 2和3不是直接好友
        }

        @Test
        @DisplayName("清空好友关系后验证状态")
        void shouldClearAllFriendRelations() {
            // Given
            friendService.addFriend(1L, 2L);
            friendService.addFriend(1L, 3L);

            // When
            friendService.clearFriendRelations();

            // Then
            assertFalse(friendService.isFriend(1L, 2L));
            assertFalse(friendService.isFriend(1L, 3L));
            assertTrue(friendService.listFriends(1L).isEmpty());
        }
    }
}
