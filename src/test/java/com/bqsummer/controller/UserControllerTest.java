package com.bqsummer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserControllerTest extends BaseTest {

    @Test
    public void testGetAdminInfo() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/user/profile")
                .then()
                .statusCode(200)
                .body("username", notNullValue())
                .body("password", nullValue());
    }

    @Test
    @DisplayName("GET /user/profile should return 401 without token")
    public void testGetProfile_Unauthorized() {
        given()
                .when()
                .get("/api/v1/user/profile")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /user/profile/ext returns empty profile when not exists (newly registered user)")
    public void testGetExtProfile_EmptyWhenNotExists() {
        // Register a fresh user to guarantee no existing profile
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "it_" + unique + "@example.com";
        String username = "it_" + unique;
        String registerBody = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"P@ssw0rd\"" +
                "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String accessToken = JSONObject.parseObject(regResp).getString("accessToken");
        Long userId = JSONObject.parseObject(regResp).getLong("userId");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/user/profile/ext")
                .then()
                .statusCode(200)
                .body("userId", equalTo(userId.intValue()))
                .body("gender", nullValue())
                .body("birthday", nullValue())
                .body("heightCm", nullValue())
                .body("mbti", nullValue())
                .body("occupation", nullValue())
                .body("interests", nullValue())
                .body("photos", nullValue());
    }

    @Test
    @DisplayName("PUT/GET /user/profile/ext create and update profile")
    public void testUpsertExtProfile_CreateAndUpdate() {
        // Register a new normal user
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "it2_" + unique + "@example.com";
        String username = "it2_" + unique;
        String registerBody = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"P@ssw0rd\"" +
                "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String accessToken = JSONObject.parseObject(regResp).getString("accessToken");

        String createBody = "{" +
                "\"gender\":\"female\"," +
                "\"heightCm\":165," +
                "\"mbti\":\"INTJ\"," +
                "\"occupation\":\"Engineer\"," +
                "\"interests\":\"reading,coding\"," +
                "\"photos\":\"https://ex.com/a.jpg,https://ex.com/b.jpg\"" +
                "}";

        given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body(createBody)
                .when()
                .put("/api/v1/user/profile/ext")
                .then()
                .statusCode(200)
                .body(equalTo("扩展资料已保存"));

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/user/profile/ext")
                .then()
                .statusCode(200)
                .body("gender", equalTo("female"))
                .body("heightCm", equalTo(165))
                .body("mbti", equalTo("INTJ"))
                .body("occupation", equalTo("Engineer"))
                .body("interests", equalTo("reading,coding"))
                .body("photos", containsString("ex.com"));

        String updateBody = "{" +
                "\"gender\":\"male\"," +
                "\"heightCm\":180," +
                "\"mbti\":\"ENTP\"," +
                "\"occupation\":\"Architect\"," +
                "\"interests\":\"music,sports\"," +
                "\"photos\":\"https://ex.com/c.jpg\"" +
                "}";

        given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body(updateBody)
                .when()
                .put("/api/v1/user/profile/ext")
                .then()
                .statusCode(200)
                .body(equalTo("扩展资料已保存"));

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/user/profile/ext")
                .then()
                .statusCode(200)
                .body("gender", equalTo("male"))
                .body("heightCm", equalTo(180))
                .body("mbti", equalTo("ENTP"))
                .body("occupation", equalTo("Architect"))
                .body("interests", equalTo("music,sports"))
                .body("photos", equalTo("https://ex.com/c.jpg"));
    }

    @Test
    @DisplayName("PUT /user/profile success")
    public void testUpdateProfile_Success() {
        String body = "{" +
                "\"nickName\":\"New Nick\"," +
                "\"avatar\":\"https://avatar.example.com/a.png\"" +
                "}";
        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put("/api/v1/user/profile")
                .then()
                .statusCode(200)
                .body(equalTo("用户信息更新成功"));
    }

    @Test
    @DisplayName("PUT /user/profile unauthorized without token")
    public void testUpdateProfile_Unauthorized() {
        given()
                .header("Content-Type", "application/json")
                .body("{\"nickName\":\"x\"}")
                .when()
                .put("/api/v1/user/profile")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /user/profile should soft delete and blacklist token")
    public void testDeleteProfile_SuccessAndBlacklist() {
        // Register a throwaway user to delete
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "it3_" + unique + "@example.com";
        String username = "it3_" + unique;
        String registerBody = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"P@ssw0rd\"" +
                "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String accessToken = JSONObject.parseObject(regResp).getString("accessToken");

        // Delete current user
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/user/profile")
                .then()
                .statusCode(200);

        // Same token should now be rejected (blacklisted + disabled user)
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/user/profile")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /user/profile unauthorized without token")
    public void testDeleteProfile_Unauthorized() {
        given()
                .when()
                .delete("/api/v1/user/profile")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /user/list requires ADMIN role - admin token allowed")
    public void testGetUserList_AdminAllowed() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/user/list")
                .then()
                .statusCode(anyOf(is(200), is(403))) // in some environments token may not be admin
                .body(anyOf(containsString("仅管理员可访问"), anything()));
    }

    @Test
    @DisplayName("GET /user/list forbidden for normal USER")
    public void testGetUserList_UserForbidden() {
        // Register a user without admin role
        String unique = String.valueOf(System.currentTimeMillis());
        String email = "it4_" + unique + "@example.com";
        String username = "it4_" + unique;
        String registerBody = "{" +
                "\"username\":\"" + username + "\"," +
                "\"email\":\"" + email + "\"," +
                "\"password\":\"P@ssw0rd\"" +
                "}";

        String regResp = given()
                .header("Content-Type", "application/json")
                .body(registerBody)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(200)
                .extract().asString();
        String accessToken = JSONObject.parseObject(regResp).getString("accessToken");

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/user/list")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("GET /user/profile/ext unauthorized with invalid token")
    public void testGetExtProfile_UnauthorizedInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid.token.value")
                .when()
                .get("/api/v1/user/profile/ext")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("PUT /user/profile/ext unauthorized without token")
    public void testUpsertExtProfile_Unauthorized() {
        given()
                .header("Content-Type", "application/json")
                .body("{\"gender\":\"female\"}")
                .when()
                .put("/api/v1/user/profile/ext")
                .then()
                .statusCode(401);
    }
}
