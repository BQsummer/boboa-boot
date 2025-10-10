package com.bqsummer.controller;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.service.im.FriendService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FriendController API测试
 * 使用RestAssured进行完整的API测试，覆盖所有代码分支
 */
@WebMvcTest(FriendController.class)
@Import(FriendControllerTest.TestConfig.class)
@DisplayName("好友管理API测试")
class FriendControllerTest {

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        FriendService friendService() {
            return mock(FriendService.class);
        }
    }

    @Autowired
    private FriendService friendService;

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor authWithUserId(long uid, String... roles) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user" + uid,
                "N/A",
                AuthorityUtils.createAuthorityList(roles)
        );
        auth.setDetails(uid);
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("添加好友测试")
    class AddFriendTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("成功添加好友")
        void shouldAddFriendSuccessfully() {
            // 模拟Security上下文返回用户ID
            doNothing().when(friendService).addFriend(1L, 2L);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
                .post("/api/v1/friends/{friendId}", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("success", equalTo(true))
                .body("code", equalTo(200))
                .body("message", equalTo("操作成功"));

            verify(friendService).addFriend(anyLong(), eq(2L));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("添加好友时传入null ID应该返回400")
        void shouldReturn400WhenFriendIdIsNull() {
            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
                .post("/api/v1/friends/{friendId}", (Object) null)
            .then()
                .status(org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("添加好友时服务层抛出异常")
        void shouldHandleServiceException() {
            doThrow(new IllegalArgumentException("不能添加自己为好友"))
                .when(friendService).addFriend(anyLong(), eq(1L));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
                .post("/api/v1/friends/{friendId}", 1L)
            .then()
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("未认证用户添加好友应返回401")
        void shouldReturn401ForUnauthenticatedUser() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
                .post("/api/v1/friends/{friendId}", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("无USER角色用户添加好友应返回403")
        void shouldReturn403ForUserWithoutUserRole() {
            given()
                .auth().with(authWithUserId(1L, "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
                .post("/api/v1/friends/{friendId}", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("删除好友测试")
    class RemoveFriendTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("成功删除好友")
        void shouldRemoveFriendSuccessfully() {
            doNothing().when(friendService).removeFriend(anyLong(), eq(2L));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .delete("/api/v1/friends/{friendId}", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("success", equalTo(true))
                .body("code", equalTo(200))
                .body("message", equalTo("操作成功"));

            verify(friendService).removeFriend(anyLong(), eq(2L));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("删除不存在的好友关系")
        void shouldHandleRemoveNonExistentFriend() {
            doThrow(new IllegalArgumentException("不是好友关系"))
                .when(friendService).removeFriend(anyLong(), eq(3L));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .delete("/api/v1/friends/{friendId}", 3L)
            .then()
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("未认证用户删除好友应返回401")
        void shouldReturn401ForUnauthenticatedUser() {
            given()
            .when()
                .delete("/api/v1/friends/{friendId}", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("获取好友列表测试")
    class ListFriendsTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("成功获取好友列表")
        void shouldListFriendsSuccessfully() {
            User friend1 = User.builder()
                .id(2L)
                .username("friend1")
                .nickname("好友1")
                .email("friend1@example.com")
                .build();

            User friend2 = User.builder()
                .id(3L)
                .username("friend2")
                .nickname("好友2")
                .email("friend2@example.com")
                .build();

            List<User> friends = Arrays.asList(friend1, friend2);
            when(friendService.listFriends(anyLong())).thenReturn(friends);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends")
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("size()", equalTo(2))
                .body("[0].id", equalTo(2))
                .body("[0].username", equalTo("friend1"))
                .body("[0].nickname", equalTo("好友1"))
                .body("[1].id", equalTo(3))
                .body("[1].username", equalTo("friend2"))
                .body("[1].nickname", equalTo("好友2"));

            verify(friendService).listFriends(anyLong());
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("获取空的好友列表")
        void shouldReturnEmptyFriendsList() {
            when(friendService.listFriends(anyLong())).thenReturn(Collections.emptyList());

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends")
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("size()", equalTo(0));

            verify(friendService).listFriends(anyLong());
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("服务层抛出异常时的处理")
        void shouldHandleServiceException() {
            when(friendService.listFriends(anyLong()))
                .thenThrow(new RuntimeException("数据库连接失败"));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends")
            .then()
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("未认证用户获取好友列表应返回401")
        void shouldReturn401ForUnauthenticatedUser() {
            given()
            .when()
                .get("/api/v1/friends")
            .then()
                .status(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("检查好友关系测试")
    class IsFriendTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("检查存在的好友关系")
        void shouldReturnTrueForExistingFriendship() {
            when(friendService.isFriend(anyLong(), eq(2L))).thenReturn(true);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends/{friendId}/status", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body(equalTo("true"));

            verify(friendService).isFriend(anyLong(), eq(2L));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("检查不存在的好友关系")
        void shouldReturnFalseForNonExistingFriendship() {
            when(friendService.isFriend(anyLong(), eq(3L))).thenReturn(false);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends/{friendId}/status", 3L)
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body(equalTo("false"));

            verify(friendService).isFriend(anyLong(), eq(3L));
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("检查好友关系时传入null ID")
        void shouldHandleNullFriendId() {
            when(friendService.isFriend(anyLong(), isNull())).thenReturn(false);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends/{friendId}/status", (Object) null)
            .then()
                .status(org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("未认证用户检查好友关系应返回401")
        void shouldReturn401ForUnauthenticatedUser() {
            given()
            .when()
                .get("/api/v1/friends/{friendId}/status", 2L)
            .then()
                .status(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("搜索用户测试")
    class SearchUsersTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("成功搜索用户")
        void shouldSearchUsersSuccessfully() {
            User user1 = User.builder()
                .id(2L)
                .username("testuser2")
                .nickname("测试用户2")
                .email("test2@example.com")
                .build();

            List<User> searchResults = Arrays.asList(user1);
            when(friendService.searchUsers(eq("test"), anyLong())).thenReturn(searchResults);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .queryParam("keyword", "test")
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(2))
                .body("[0].username", equalTo("testuser2"))
                .body("[0].nickname", equalTo("测试用户2"));

            verify(friendService).searchUsers(eq("test"), anyLong());
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("搜索无结果")
        void shouldReturnEmptyResultsWhenNoUsersFound() {
            when(friendService.searchUsers(eq("nonexistent"), anyLong()))
                .thenReturn(Collections.emptyList());

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .queryParam("keyword", "nonexistent")
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("size()", equalTo(0));

            verify(friendService).searchUsers(eq("nonexistent"), anyLong());
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("搜索关键词为空")
        void shouldHandleEmptyKeyword() {
            when(friendService.searchUsers(eq(""), anyLong()))
                .thenThrow(new IllegalArgumentException("搜索关键词不能为空"));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .queryParam("keyword", "")
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("缺少搜索关键词参数")
        void shouldHandleMissingKeywordParameter() {
            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("搜索关键词包含特殊字符")
        void shouldHandleSpecialCharactersInKeyword() {
            User user1 = User.builder()
                .id(2L)
                .username("user@test")
                .nickname("特殊@用户")
                .build();

            List<User> searchResults = Arrays.asList(user1);
            when(friendService.searchUsers(eq("@test"), anyLong())).thenReturn(searchResults);

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
                .queryParam("keyword", "@test")
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.OK)
                .body("size()", equalTo(1))
                .body("[0].username", equalTo("user@test"));
        }

        @Test
        @DisplayName("未认证用户搜索用户应返回401")
        void shouldReturn401ForUnauthenticatedUser() {
            given()
                .queryParam("keyword", "test")
            .when()
                .get("/api/v1/friends/search")
            .then()
                .status(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("路径参数验证测试")
    class PathParameterValidationTests {

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("负数好友ID")
        void shouldHandleNegativeFriendId() {
            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .post("/api/v1/friends/{friendId}", -1L)
            .then()
                // 控制器未对负数ID做校验，因此应返回200
                .status(org.springframework.http.HttpStatus.OK);
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("超大好友ID")
        void shouldHandleLargeFriendId() {
            doNothing().when(friendService).addFriend(anyLong(), eq(Long.MAX_VALUE));

            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .post("/api/v1/friends/{friendId}", Long.MAX_VALUE)
            .then()
                .status(org.springframework.http.HttpStatus.OK);
        }

        @Test
        @WithMockUser(authorities = "ROLE_USER")
        @DisplayName("非数字好友ID")
        void shouldHandleNonNumericFriendId() {
            given()
                .auth().with(authWithUserId(1L, "ROLE_USER"))
            .when()
                .post("/api/v1/friends/{friendId}", "abc")
            .then()
                .status(org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
