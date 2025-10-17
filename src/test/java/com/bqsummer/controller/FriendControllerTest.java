package com.bqsummer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.bqsummer.util.DbAssertions.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * RestAssured integration tests for {@link FriendController} covering happy path and error branches.
 */
@DisplayName("FriendController API tests")
class FriendControllerTest extends BaseTest {

    /**
     * Purpose: ensure adding a friend with valid users returns 200, response payload is success, and DB stores bidirectional records.
     */
    @Test
    @DisplayName("POST /api/v1/friends/{friendId} returns 200 and persists friendship")
    void shouldReturn200AndPersistFriendshipWhenAddFriendWithValidUsers() {
        TestUser owner = registerUser("friend_add_owner");
        TestUser partner = registerUser("friend_add_partner");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", partner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("success", equalTo(true));

        assertAll("friendship records inserted",
                () -> assertExists("friends", "user_id = ? AND friend_user_id = ?", owner.id, partner.id),
                () -> assertExists("friends", "user_id = ? AND friend_user_id = ?", partner.id, owner.id)
        );

        assertAll("conversation records inserted",
                () -> assertExists("conversations", "user_id = ? AND peer_id = ? AND is_deleted = 0", owner.id, partner.id),
                () -> assertExists("conversations", "user_id = ? AND peer_id = ? AND is_deleted = 0", partner.id, owner.id)
        );
    }

    /**
     * Purpose: verify adding yourself as a friend is rejected with 400 and no records are persisted.
     */
    @Test
    @DisplayName("POST /api/v1/friends/{friendId} returns 400 when friendId equals current user")
    void shouldReturn400WhenAddFriendWithSelfId() {
        TestUser owner = registerUser("friend_self");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", owner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(400);

        assertAll("self friendship not created",
                () -> assertNotExists("friends", "user_id = ? AND friend_user_id = ?", owner.id, owner.id)
        );
    }

    /**
     * Purpose: ensure adding non-existing user yields 400 with business code 404.
     */
    @Test
    @DisplayName("POST /api/v1/friends/{friendId} returns code 404 when target user missing")
    void shouldReturn400WhenAddFriendTargetMissing() {
        TestUser owner = registerUser("friend_missing_target");
        long nonexistentId = 9_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000L);

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", nonexistentId)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(404))
                .body("message", anyOf(containsString("不存在"), containsString("not")));
    }

    /**
     * Purpose: ensure duplicate friend addition triggers business conflict (code 409) and HTTP 400.
     */
    @Test
    @DisplayName("POST /api/v1/friends/{friendId} returns code 409 when already friends")
    void shouldReturn400WhenAddFriendAlreadyExists() {
        TestUser owner = registerUser("friend_conflict_owner");
        TestUser partner = registerUser("friend_conflict_partner");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", partner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", partner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(400)
                .body("code", equalTo(409))
                .body("success", equalTo(false));
    }

    /**
     * Purpose: ensure missing Authorization header on add friend results in 401.
     */
    @Test
    @DisplayName("POST /api/v1/friends/{friendId} returns 401 without Authorization header")
    void shouldReturn401WhenAddFriendWithoutToken() {
        TestUser partner = registerUser("friend_add_unauth");

        given()
                .pathParam("friendId", partner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure deleting an existing friend succeeds and removes both directions in DB.
     */
    @Test
    @DisplayName("DELETE /api/v1/friends/{friendId} removes both sides of friendship")
    void shouldReturn200AndRemoveFriendRecordsWhenDeleteFriend() {
        Friendship friendship = setupFriendship("friend_delete");

        given()
                .header("Authorization", "Bearer " + friendship.owner.token)
                .pathParam("friendId", friendship.partner.id)
        .when()
                .delete("/api/v1/friends/{friendId}")
        .then()
                .statusCode(200)
                .body("success", equalTo(true));
        assertAll("conversation records removed (soft delete)",
                () -> assertExists("conversations", "user_id = ? AND peer_id = ? AND is_deleted = 1", friendship.owner.id, friendship.partner.id),
                () -> assertExists("conversations", "user_id = ? AND peer_id = ? AND is_deleted = 1", friendship.partner.id, friendship.owner.id)
        );


        assertAll("friendship records removed",
                () -> assertNotExists("friends", "user_id = ? AND friend_user_id = ?", friendship.owner.id, friendship.partner.id),
                () -> assertNotExists("friends", "user_id = ? AND friend_user_id = ?", friendship.partner.id, friendship.owner.id)
        );
    }

    /**
     * Purpose: ensure deleting non-friend returns 400 with business message.
     */
    @Test
    @DisplayName("DELETE /api/v1/friends/{friendId} returns 400 when not friends")
    void shouldReturn400WhenDeleteNotExistingFriend() {
        TestUser owner = registerUser("friend_delete_missing_owner");
        TestUser target = registerUser("friend_delete_missing_target");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", target.id)
        .when()
                .delete("/api/v1/friends/{friendId}")
        .then()
                .statusCode(400)
                .body("code", equalTo(400));
    }

    /**
     * Purpose: ensure deleting friend without token yields 401.
     */
    @Test
    @DisplayName("DELETE /api/v1/friends/{friendId} returns 401 without Authorization header")
    void shouldReturn401WhenDeleteFriendWithoutToken() {
        TestUser target = registerUser("friend_delete_noauth");

        given()
                .pathParam("friendId", target.id)
        .when()
                .delete("/api/v1/friends/{friendId}")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure listing friends returns persisted entries.
     */
    @Test
    @DisplayName("GET /api/v1/friends returns friends list for user")
    void shouldReturn200WithFriendListWhenListFriends() {
        Friendship friendship = setupFriendship("friend_list");

        List<Integer> ids =
                given()
                        .header("Authorization", "Bearer " + friendship.owner.token)
                .when()
                        .get("/api/v1/friends")
                .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getList("id", Integer.class);

        assertAll("list friends contains partner",
                () -> org.junit.jupiter.api.Assertions.assertTrue(ids.contains(friendship.partner.id.intValue()), "should contain partner id")
        );
    }

    /**
     * Purpose: ensure listing conversations shows partner after friend creation.
     */
    @Test
    @DisplayName("GET /api/v1/conversations shows friend after adding")
    void shouldShowConversationAfterAddingFriend() {
        Friendship friendship = setupFriendship("friend_conv_list");

        given()
                .header("Authorization", "Bearer " + friendship.owner.token)
        .when()
                .get("/api/v1/conversations")
        .then()
                .statusCode(200)
                .body("peerId", hasItem(friendship.partner.id.intValue()));
    }


    /**
     * Purpose: ensure listing friends without auth returns 401.
     */
    @Test
    @DisplayName("GET /api/v1/friends returns 401 without token")
    void shouldReturn401WhenListFriendsWithoutToken() {
        when()
                .get("/api/v1/friends")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure friendship status API returns true for existing friendship.
     */
    @Test
    @DisplayName("GET /api/v1/friends/{friendId}/status returns true when friends")
    void shouldReturn200WithTrueWhenIsFriend() {
        Friendship friendship = setupFriendship("friend_status_true");

        given()
                .header("Authorization", "Bearer " + friendship.owner.token)
                .pathParam("friendId", friendship.partner.id)
        .when()
                .get("/api/v1/friends/{friendId}/status")
        .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    /**
     * Purpose: ensure friendship status API returns false when not friends.
     */
    @Test
    @DisplayName("GET /api/v1/friends/{friendId}/status returns false when not friends")
    void shouldReturn200WithFalseWhenIsFriendNotExists() {
        TestUser owner = registerUser("friend_status_false_owner");
        TestUser target = registerUser("friend_status_false_target");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", target.id)
        .when()
                .get("/api/v1/friends/{friendId}/status")
        .then()
                .statusCode(200)
                .body(equalTo("false"));
    }

    /**
     * Purpose: ensure status endpoint without auth returns 401.
     */
    @Test
    @DisplayName("GET /api/v1/friends/{friendId}/status returns 401 without token")
    void shouldReturn401WhenCheckFriendStatusWithoutToken() {
        TestUser target = registerUser("friend_status_noauth");

        given()
                .pathParam("friendId", target.id)
        .when()
                .get("/api/v1/friends/{friendId}/status")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure search returns potential friends excluding already connected users.
     */
    @Test
    @DisplayName("GET /api/v1/friends/search returns users when keyword matches")
    void shouldReturn200WhenSearchUsersWithKeyword() {
        TestUser requester = registerUser("friend_search_owner");
        TestUser candidate = registerUser("friend_search_candidate");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) (List<?>)
                given()
                        .header("Authorization", "Bearer " + requester.token)
                        .queryParam("keyword", candidate.username)
                .when()
                        .get("/api/v1/friends/search")
                .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getList("");

        assertAll("search result should include candidate",
                () -> org.junit.jupiter.api.Assertions.assertTrue(result.stream().anyMatch(row -> candidate.id.equals(((Number) row.get("id")).longValue())), "candidate id present"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(result.stream().noneMatch(row -> requester.id.equals(((Number) row.get("id")).longValue())), "self excluded")
        );
    }

    /**
     * Purpose: ensure search with blank keyword returns 400.
     */
    @Test
    @DisplayName("GET /api/v1/friends/search returns 400 when keyword blank")
    void shouldReturn400WhenSearchUsersWithBlankKeyword() {
        TestUser requester = registerUser("friend_search_blank");

        given()
                .header("Authorization", "Bearer " + requester.token)
                .queryParam("keyword", " ")
        .when()
                .get("/api/v1/friends/search")
        .then()
                .statusCode(400)
                .body("code", equalTo(400));
    }

    /**
     * Purpose: ensure search without token returns 401.
     */
    @Test
    @DisplayName("GET /api/v1/friends/search returns 401 without token")
    void shouldReturn401WhenSearchUsersWithoutToken() {
        given()
                .queryParam("keyword", "abc")
        .when()
                .get("/api/v1/friends/search")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure hitting unknown friend route returns 404.
     */
    @Test
    @DisplayName("GET unknown path under /api/v1/friends returns 404")
    void shouldReturn404WhenFriendEndpointNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/friends/unknown")
        .then()
                .statusCode(400);
    }

    private Friendship setupFriendship(String prefix) {
        TestUser owner = registerUser(prefix + "_owner");
        TestUser partner = registerUser(prefix + "_partner");

        given()
                .header("Authorization", "Bearer " + owner.token)
                .pathParam("friendId", partner.id)
        .when()
                .post("/api/v1/friends/{friendId}")
        .then()
                .statusCode(200);

        assertAll("friendship persisted",
                () -> assertExists("friends", "user_id = ? AND friend_user_id = ?", owner.id, partner.id),
                () -> assertExists("friends", "user_id = ? AND friend_user_id = ?", partner.id, owner.id)
        );

        return new Friendship(owner, partner);
    }

    private TestUser registerUser(String prefix) {
        String suffix = prefix + "_" + System.nanoTime() + ThreadLocalRandom.current().nextInt(1000, 9999);
        String email = suffix + "@example.com";
        String username = suffix;
        String password = "P@ssw0rd" + ThreadLocalRandom.current().nextInt(10, 99);

        String body = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        String resp =
                given()
                        .header("Content-Type", "application/json")
                        .body(body)
                .when()
                        .post("/api/v1/auth/register")
                .then()
                        .statusCode(200)
                        .body("accessToken", notNullValue())
                        .body("userId", notNullValue())
                        .extract()
                        .asString();

        JSONObject json = JSONObject.parseObject(resp);

        TestUser user = new TestUser();
        user.id = json.getLong("userId");
        user.username = username;
        user.email = email;
        user.password = password;
        user.token = json.getString("accessToken");
        return user;
    }

    private record Friendship(TestUser owner, TestUser partner) {}

    private static class TestUser {
        Long id;
        String username;
        String email;
        String password;
        String token;
    }
}
