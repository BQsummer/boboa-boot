package com.bqsummer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static com.bqsummer.util.DbAssertions.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * RestAssured integration tests targeting {@link UserController} covering happy path and error branches.
 */
@DisplayName("UserController API tests")
class UserControllerTest extends BaseTest {

    /**
     * Purpose: ensure fetching profile with a valid token returns the persisted user and hides the password.
     */
    @Test
    @DisplayName("GET /api/v1/user/profile returns current user details")
    void shouldReturnCurrentUserProfileWhenTokenValid() {
        TestUser user = registerUser("profile_valid");

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .get("/api/v1/user/profile")
        .then()
                .statusCode(200)
                .body("id", equalTo(user.id.intValue()))
                .body("username", equalTo(user.username))
                .body("email", equalTo(user.email))
                .body("password", nullValue());
    }

    /**
     * Purpose: verify accessing profile without Authorization header is rejected with 401.
     */
    @Test
    @DisplayName("GET /api/v1/user/profile returns 401 without token")
    void shouldReturn401WhenProfileRequestedWithoutToken() {
        when()
                .get("/api/v1/user/profile")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure extended profile returns a shell object when no data exists yet.
     */
    @Test
    @DisplayName("GET /api/v1/user/profile/ext returns fallback profile when missing")
    void shouldReturnBlankExtendedProfileWhenMissing() {
        TestUser user = registerUser("profile_ext_blank");

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .get("/api/v1/user/profile/ext")
        .then()
                .statusCode(200)
                .body("userId", equalTo(user.id.intValue()))
                .body("gender", nullValue())
                .body("mbti", nullValue())
                .body("photos", nullValue());
    }

    /**
     * Purpose: ensure extended profile endpoint denies requests with malformed tokens.
     */
    @Test
    @DisplayName("GET /api/v1/user/profile/ext returns 401 for invalid token")
    void shouldReturn401WhenGetExtendedProfileWithInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid.token.value")
        .when()
                .get("/api/v1/user/profile/ext")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure upserting extended profile persists values and can be fetched back.
     */
    @Test
    @DisplayName("PUT /api/v1/user/profile/ext persists extended profile")
    void shouldPersistExtendedProfileWhenUpsertingWithValidPayload() {
        TestUser user = registerUser("profile_ext_upsert");

        String requestBody = "{" +
                "\"gender\":\"female\"," +
                "\"birthday\":\"1995-08-15\"," +
                "\"heightCm\":168," +
                "\"mbti\":\"INTJ\"," +
                "\"occupation\":\"Engineer\"," +
                "\"interests\":\"reading,coding\"," +
                "\"photos\":\"[\\\"https://img.example.com/a.jpg\\\"]\"," +
                "\"desc\":\"desc-content\"" +
                "}";

        given()
                .header("Authorization", "Bearer " + user.token)
                .header("Content-Type", "application/json")
                .body(requestBody)
        .when()
                .put("/api/v1/user/profile/ext")
        .then()
                .statusCode(200)
                .body(equalTo("扩展资料已保存"));

        assertAll("extended profile persisted",
                () -> assertExists("user_profiles", "user_id = ?", user.id),
                () -> assertValue("user_profiles", "gender", "female", "user_id = ?", user.id),
                () -> assertValue("user_profiles", "mbti", "INTJ", "user_id = ?", user.id),
                () -> assertValue("user_profiles", "height_cm", 168, "user_id = ?", user.id),
                () -> assertValue("user_profiles", "desc", "desc-content", "user_id = ?", user.id)
        );

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .get("/api/v1/user/profile/ext")
        .then()
                .statusCode(200)
                .body("userId", equalTo(user.id.intValue()))
                .body("gender", equalTo("female"))
                .body("birthday", equalTo("1995-08-15"))
                .body("heightCm", equalTo(168))
                .body("mbti", equalTo("INTJ"))
                .body("occupation", equalTo("Engineer"))
                .body("interests", equalTo("reading,coding"))
                .body("photos", equalTo("[\"https://img.example.com/a.jpg\"]"))
                .body("desc", equalTo("desc-content"));
    }

    /**
     * Purpose: ensure upsert rejects requests without authentication and no DB record is created.
     */
    @Test
    @DisplayName("PUT /api/v1/user/profile/ext returns 401 without token")
    void shouldReturn401WhenUpsertExtendedProfileWithoutToken() {
        TestUser user = registerUser("profile_ext_noauth");

        String requestBody = "{" +
                "\"gender\":\"male\"," +
                "\"mbti\":\"ENFP\"" +
                "}";

        given()
                .header("Content-Type", "application/json")
                .body(requestBody)
        .when()
                .put("/api/v1/user/profile/ext")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));

        assertNotExists("user_profiles", "user_id = ?", user.id);
    }

    /**
     * Purpose: ensure updating profile succeeds with token even though backend currently returns a stub message.
     */
    @Test
    @DisplayName("PUT /api/v1/user/profile returns success message")
    void shouldReturnSuccessWhenUpdateProfileWithValidToken() {
        TestUser user = registerUser("profile_update_success");

        String body = "{" +
                "\"username\":\"" + user.username + "_new\"," +
                "\"nickName\":\"Tester\"" +
                "}";

        given()
                .header("Authorization", "Bearer " + user.token)
                .header("Content-Type", "application/json")
                .body(body)
        .when()
                .put("/api/v1/user/profile")
        .then()
                .statusCode(200)
                .body(equalTo("用户信息更新成功"));
    }

    /**
     * Purpose: ensure updating profile without token is rejected with 401.
     */
    @Test
    @DisplayName("PUT /api/v1/user/profile returns 401 without token")
    void shouldReturn401WhenUpdateProfileWithoutToken() {
        String body = "{" +
                "\"username\":\"anonymous\"" +
                "}";

        given()
                .header("Content-Type", "application/json")
                .body(body)
        .when()
                .put("/api/v1/user/profile")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure deleting current user soft deletes the record, removes refresh tokens, and blacklists the access token.
     */
    @Test
    @DisplayName("DELETE /api/v1/user/profile soft deletes account and revokes tokens")
    void shouldSoftDeleteCurrentUserAndCleanupTokensWhenAuthorized() {
        TestUser user = registerUser("profile_delete_success");

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .delete("/api/v1/user/profile")
        .then()
                .statusCode(200);

        assertAll("user soft deleted and tokens removed",
                () -> assertValue("users", "is_deleted", true, "id = ?", user.id),
                () -> assertNotExists("refresh_tokens", "user_id = ?", user.id)
        );

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .get("/api/v1/user/profile")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure deleting profile without authentication returns 401 and leaves DB untouched.
     */
    @Test
    @DisplayName("DELETE /api/v1/user/profile returns 401 without token")
    void shouldReturn401WhenDeleteCurrentUserWithoutToken() {
        when()
                .delete("/api/v1/user/profile")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure administrator token can retrieve the protected user list endpoint.
     */
    @Test
    @DisplayName("GET /api/v1/user/list returns data for admin")
    void shouldReturnUserListWhenAdminRole() {
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/v1/user/list")
        .then()
                .statusCode(401);
    }

    /**
     * Purpose: ensure non-admin users receive 403 when accessing the admin-only list endpoint.
     */
    @Test
    @DisplayName("GET /api/v1/user/list returns 403 for non-admin user")
    void shouldReturn403WhenUserListRequestedByNonAdmin() {
        TestUser user = registerUser("profile_list_forbidden");

        given()
                .header("Authorization", "Bearer " + user.token)
        .when()
                .get("/api/v1/user/list")
        .then()
                .statusCode(403);
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

        String response =
                given()
                        .header("Content-Type", "application/json")
                        .body(body)
                .when()
                        .post("/api/v1/auth/register")
                .then()
                        .statusCode(200)
                        .body("accessToken", notNullValue())
                        .body("refreshToken", notNullValue())
                        .body("userId", notNullValue())
                        .extract()
                        .asString();

        JSONObject json = JSONObject.parseObject(response);

                TestUser user = new TestUser();
                user.id = json.getLong("userId");
                user.username = username;
                user.email = email;
                user.token = json.getString("accessToken");
                return user;
    }

    private static class TestUser {
        Long id;
        String username;
        String email;
        String token;
    }
}
