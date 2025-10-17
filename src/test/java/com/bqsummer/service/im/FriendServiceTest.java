package com.bqsummer.service.im;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.im.Friend;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.FriendMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.ConversationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ConversationMapper conversationMapper;

    @InjectMocks
    private FriendService friendService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).username("user1").build();
        user2 = User.builder().id(2L).username("user2").build();
    }

    @Test
    @DisplayName("添加好友成功时写入双向关系")
    void shouldAddFriendSuccessfully() {
        when(userMapper.findById(1L)).thenReturn(user1);
        when(userMapper.findById(2L)).thenReturn(user2);
        when(friendMapper.selectCount(any())).thenReturn(0L);
        when(friendMapper.insert(any(Friend.class))).thenReturn(1);

        friendService.addFriend(1L, 2L);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper, times(2)).insert(captor.capture());
        List<Friend> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved)
                .extracting(Friend::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(saved)
                .extracting(Friend::getFriendUserId)
                .containsExactlyInAnyOrder(1L, 2L);

        // verify conversation upserts were called for both directions
        verify(conversationMapper, times(1)).insertOrRestore(1L, 2L);
        verify(conversationMapper, times(1)).insertOrRestore(2L, 1L);
    }

    @Test
    @DisplayName("添加自己为好友时抛出异常")
    void shouldThrowWhenAddSelf() {
        SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.addFriend(1L, 1L));
        assertThat(ex.getCode()).isEqualTo(400);
        verifyNoInteractions(friendMapper);
    }

    @Test
    @DisplayName("好友不存在时抛出404异常")
    void shouldThrowWhenFriendNotFound() {
        when(userMapper.findById(1L)).thenReturn(user1);
        when(userMapper.findById(2L)).thenReturn(null);

        SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.addFriend(1L, 2L));
        assertThat(ex.getCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("已是好友时抛出409异常")
    void shouldThrowWhenAlreadyFriend() {
        when(userMapper.findById(1L)).thenReturn(user1);
        when(userMapper.findById(2L)).thenReturn(user2);
        when(friendMapper.selectCount(any())).thenReturn(1L);

        SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.addFriend(1L, 2L));
        assertThat(ex.getCode()).isEqualTo(409);
    verify(friendMapper, never()).insert(any(Friend.class));
    }

    @Test
    @DisplayName("删除好友时会删除双向记录")
    void shouldRemoveFriendSuccessfully() {
        when(friendMapper.selectCount(any())).thenReturn(1L);

        friendService.removeFriend(1L, 2L);

        verify(friendMapper, times(2)).delete(any());
    }

    @Test
    @DisplayName("删除不存在的好友返回异常")
    void shouldThrowWhenRemovingNonFriend() {
        when(friendMapper.selectCount(any())).thenReturn(0L);

        SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.removeFriend(1L, 2L));
        assertThat(ex.getCode()).isEqualTo(400);
        verify(friendMapper, never()).delete(any());
    }

    @Test
    @DisplayName("列出好友时参数为空抛异常")
    void shouldThrowWhenListFriendsWithNullUser() {
        SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.listFriends(null));
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("可以列出好友列表")
    void shouldListFriends() {
        when(friendMapper.findFriendUsers(1L)).thenReturn(List.of(user2));

        List<User> friends = friendService.listFriends(1L);

        assertThat(friends).containsExactly(user2);
    }

    @Nested
    @DisplayName("搜索用户")
    class SearchUsers {

        @Test
        @DisplayName("关键词为空抛出异常")
        void shouldThrowWhenKeywordBlank() {
            SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.searchUsers(" ", 1L));
            assertThat(ex.getCode()).isEqualTo(400);
        }

        @Test
        @DisplayName("当前用户ID为空抛出异常")
        void shouldThrowWhenCurrentUserIdNull() {
            SnorlaxClientException ex = assertThrows(SnorlaxClientException.class, () -> friendService.searchUsers("abc", null));
            assertThat(ex.getCode()).isEqualTo(400);
        }

        @Test
        @DisplayName("搜索结果会排除已有好友")
        void shouldExcludeExistingFriends() {
            when(friendMapper.selectList(any())).thenReturn(List.of(buildFriend(1L, 2L)));
            when(userMapper.searchUsers(eq("tom"), eq(1L))).thenReturn(List.of(user2, User.builder().id(3L).username("user3").build()));

            List<User> result = friendService.searchUsers("tom", 1L);

            assertThat(result)
                    .extracting(User::getId)
                    .containsExactly(3L);
        }

        private Friend buildFriend(Long userId, Long friendId) {
            Friend f = new Friend();
            f.setUserId(userId);
            f.setFriendUserId(friendId);
            return f;
        }
    }

    @Test
    @DisplayName("判断好友关系")
    void shouldDetectFriendship() {
        when(friendMapper.selectCount(any())).thenReturn(1L);
        assertThat(friendService.isFriend(1L, 2L)).isTrue();

        when(friendMapper.selectCount(any())).thenReturn(0L);
        assertThat(friendService.isFriend(1L, 2L)).isFalse();

        assertThat(friendService.isFriend(1L, 1L)).isFalse();
    }
}
