package com.bqsummer.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.BaseTest;
import com.bqsummer.util.DbAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.bqsummer.util.DbAssertions.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RestAssured integration tests targeting {@link FeedbackController} covering happy-path and error branches.
 */
@DisplayName("FeedbackController API tests")
class FeedbackControllerTest extends BaseTest {

    /**
     * Purpose: ensure submitting feedback with full payload succeeds, persists DB row, and enriches metadata.
     */
    @Test
    @DisplayName("POST /api/v1/feedback/submit persists feedback and enriches metadata")
    void shouldPersistFeedbackWhenSubmitWithValidPayload() {
        TestUser user = registerUser("feedback_submit_success");
        String suffix = uniqueSuffix("submit_ok");
        String contact = suffix + "@example.com";

        JSONObject payload = new JSONObject();
        payload.put("type", "bug");
        payload.put("content", "Button crashes on click - " + suffix);
        payload.put("contact", contact);
        payload.put("images", List.of("https://cdn.example.com/" + suffix + ".png"));
        payload.put("appVersion", "1.2.3");
        payload.put("osVersion", "macOS 14.6");
        payload.put("deviceModel", "MacBookPro18,4");
        payload.put("networkType", "wifi");
        payload.put("pageRoute", "/settings/profile");
        payload.put("extraData", Map.of("scene", "submit", "channel", "web"));

        String response =
                given()
                        .header("Authorization", "Bearer " + user.token)
                        .header("Content-Type", "application/json")
                        .header("X-Forwarded-For", "10.10.0.1")
                        .header("User-Agent", "JUnit-Agent/1.0")
                        .body(payload.toJSONString())
                .when()
                        .post("/api/v1/feedback/submit")
                .then()
                        .statusCode(200)
                        .body("id", notNullValue())
                        .extract()
                        .asString();

        Long feedbackId = JSONObject.parseObject(response).getLong("id");

        assertAll("feedback persisted with metadata",
                () -> assertExists("feedback", "id = ?", feedbackId),
                () -> assertValue("feedback", "type", "bug", "id = ?", feedbackId),
                () -> assertValue("feedback", "contact", contact, "id = ?", feedbackId),
                () -> assertValue("feedback", "status", "NEW", "id = ?", feedbackId),
                () -> assertValue("feedback", "user_id", user.id, "id = ?", feedbackId),
                () -> assertValue("feedback", "images", "[\"https://cdn.example.com/" + suffix + ".png\"]", "id = ?", feedbackId)
        );

        Map<String, Object> row = DbAssert.fetchOne("feedback", "id = ?", feedbackId);
        assertFalse(row.isEmpty(), "feedback row should be readable");
        String extraJson = (String) row.get("extra_data");
        JSONObject extra = JSONObject.parseObject(extraJson);
        assertAll("extra data merged with client info",
                () -> assertEquals("10.10.0.1", extra.getString("clientIp")),
                () -> assertEquals("JUnit-Agent/1.0", extra.getString("userAgent")),
                () -> assertEquals("submit", extra.getString("scene")),
                () -> assertEquals("web", extra.getString("channel"))
        );
    }

    /**
     * Purpose: ensure missing mandatory content triggers validation failure and no DB row is inserted.
     */
    @Test
    @DisplayName("POST /api/v1/feedback/submit returns 400 for missing content")
    void shouldReturn400WhenSubmitWithMissingRequiredFields() {
        TestUser user = registerUser("feedback_submit_missing");
        String suffix = uniqueSuffix("submit_missing");
        String contact = suffix + "@example.com";

        JSONObject payload = new JSONObject();
        payload.put("type", "bug");
        payload.put("content", "");
        payload.put("contact", contact);

        given()
                .header("Authorization", "Bearer " + user.token)
                .header("Content-Type", "application/json")
                .body(payload.toJSONString())
        .when()
                .post("/api/v1/feedback/submit")
        .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(1001))
                .body("message", containsString("反馈内容不能为空"));

        assertNotExists("feedback", "contact = ?", contact);
    }

    /**
     * Purpose: ensure oversized type exceeds DB column limit causing a server error without persisting data.
     */
    @Test
    @DisplayName("POST /api/v1/feedback/submit returns 500 when DB rejects oversized type")
    void shouldReturn500WhenSubmitWithTypeExceedingDatabaseLimit() {
        TestUser user = registerUser("feedback_submit_db_error");
        String suffix = uniqueSuffix("submit_db_error");
        String contact = suffix + "@example.com";
        String longType = "type_" + "x".repeat(80);

        JSONObject payload = new JSONObject();
        payload.put("type", longType);
        payload.put("content", "Crash report - " + suffix);
        payload.put("contact", contact);

        given()
                .header("Authorization", "Bearer " + user.token)
                .header("Content-Type", "application/json")
                .body(payload.toJSONString())
        .when()
                .post("/api/v1/feedback/submit")
        .then()
                .statusCode(500)
                .body("success", equalTo(false))
                .body("code", equalTo(1001))
                .body("message", containsString("系统忙不过来啦"));

        assertNotExists("feedback", "contact = ?", contact);
    }

    /**
     * Purpose: ensure unauthenticated submit request is rejected with 401 and nothing is stored.
     */
    @Test
    @DisplayName("POST /api/v1/feedback/submit returns 401 without token")
    void shouldReturn401WhenSubmitWithoutToken() {
        String suffix = uniqueSuffix("submit_unauth");
        String contact = suffix + "@example.com";

        JSONObject payload = new JSONObject();
        payload.put("type", "bug");
        payload.put("content", "Unauth content " + suffix);
        payload.put("contact", contact);

        given()
                .header("Content-Type", "application/json")
                .body(payload.toJSONString())
        .when()
                .post("/api/v1/feedback/submit")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));

        assertNotExists("feedback", "contact = ?", contact);
    }

    /**
     * Purpose: ensure listing feedback with filters returns only matching records for the current user.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my filters by type and status")
    void shouldReturnPagedFeedbacksWhenListWithFilters() {
        TestUser user = registerUser("feedback_list_filter");
        String bugContact = uniqueSuffix("filter_bug") + "@example.com";
        String uxContact = uniqueSuffix("filter_ux") + "@example.com";

        Long bugId = submitFeedback(user, "bug", "Bug content for filter", bugContact, "/bug", "web");
        submitFeedback(user, "ux", "UX content for filter", uxContact, "/ux", "web");

        List<Long> ids =
                given()
                        .header("Authorization", "Bearer " + user.token)
                        .queryParam("type", "bug")
                        .queryParam("status", "NEW")
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                .when()
                        .get("/api/v1/feedback/my")
                .then()
                        .statusCode(200)
                        .body("current", equalTo(1))
                        .body("size", equalTo(10))
                        .body("records", hasSize(greaterThanOrEqualTo(1)))
                        .body("records.type", everyItem(equalTo("bug")))
                        .extract()
                        .jsonPath()
                        .getList("records.id", Long.class);

        assertAll("filter results contain bug only",
                () -> assertTrue(ids.contains(bugId), "bug record should be returned"),
                () -> assertTrue(ids.stream().allMatch(bugId::equals), "should not include other records")
        );
    }

    /**
     * Purpose: ensure filter combination without matches returns empty page instead of error.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my returns empty list when filter misses")
    void shouldReturnEmptyWhenListWithNonMatchingFilter() {
        TestUser user = registerUser("feedback_list_empty");
        submitFeedback(user, "bug", "Content for empty filter", uniqueSuffix("empty") + "@example.com", "/screen", "web");

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("type", "suggestion")
                .queryParam("status", "RESOLVED")
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(200)
                .body("records", hasSize(0))
                .body("total", equalTo(0));
    }

    /**
     * Purpose: ensure invalid pagination parameters trigger 400 with structured error payload.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my returns 400 for invalid pagination params")
    void shouldReturn400WhenListWithInvalidPageParameter() {
        TestUser user = registerUser("feedback_list_invalid_page");

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", "abc")
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(1001))
                .body("message", containsString("abc"));

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", 1)
                .queryParam("size", "oops")
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(1001))
                .body("message", containsString("oops"));
    }

    /**
     * Purpose: ensure boundary pagination values (0, negative, huge) are handled gracefully.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my handles boundary pagination values")
    void shouldHandleBoundaryPaginationParameters() {
        TestUser user = registerUser("feedback_list_boundary");
        submitFeedback(user, "bug", "Boundary test content", uniqueSuffix("boundary") + "@example.com", "/boundary", "web");

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", 0)
                .queryParam("size", 5)
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(200)
                .body("current", equalTo(1))
                .body("size", equalTo(5))
                .body("records", not(empty()));

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", -1)
                .queryParam("size", 5)
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(200)
                .body("current", equalTo(1))
                .body("records", not(empty()));

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", 1)
                .queryParam("size", 0)
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(200)
                .body("size", equalTo(0))
                .body("records", hasSize(0));

        given()
                .header("Authorization", "Bearer " + user.token)
                .queryParam("page", 1)
                .queryParam("size", 500)
        .when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(200)
                .body("size", equalTo(500))
                .body("records", not(empty()));
    }

    /**
     * Purpose: ensure listing without token fails with 401 and no payload is returned.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my returns 401 without token")
    void shouldReturn401WhenListFeedbacksWithoutToken() {
        when()
                .get("/api/v1/feedback/my")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    /**
     * Purpose: ensure owner can retrieve feedback details including stored fields.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my/{id} returns detail for owner")
    void shouldReturnFeedbackDetailWhenOwnerRequests() {
        TestUser user = registerUser("feedback_detail_owner");
        String contact = uniqueSuffix("detail_owner") + "@example.com";
        Long feedbackId = submitFeedback(user, "bug", "Detail content", contact, "/detail", "web");
        int feedbackIdInt = Math.toIntExact(feedbackId);

        given()
                .header("Authorization", "Bearer " + user.token)
                .pathParam("id", feedbackId)
        .when()
                .get("/api/v1/feedback/my/{id}")
        .then()
                .statusCode(200)
                .body("id", equalTo(feedbackIdInt))
                .body("type", equalTo("bug"))
                .body("content", equalTo("Detail content"))
                .body("status", equalTo("NEW"));
    }

    /**
     * Purpose: ensure accessing another user's feedback returns 404 without leaking data.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my/{id} returns 404 for different user")
    void shouldReturn404WhenDetailRequestedByDifferentUser() {
        TestUser owner = registerUser("feedback_detail_forbidden_owner");
        Long feedbackId = submitFeedback(owner, "bug", "Forbidden detail", uniqueSuffix("detail_forbidden") + "@example.com", "/detail", "web");
        TestUser intruder = registerUser("feedback_detail_forbidden_intruder");

        given()
                .header("Authorization", "Bearer " + intruder.token)
                .pathParam("id", feedbackId)
        .when()
                .get("/api/v1/feedback/my/{id}")
        .then()
                .statusCode(404);
    }

    /**
     * Purpose: ensure requesting a missing feedback id returns 404 for the owner.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my/{id} returns 404 when feedback missing")
    void shouldReturn404WhenDetailRequestedForMissingFeedback() {
        TestUser user = registerUser("feedback_detail_missing");
        long missingId = 9_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000L);
        assertNotExists("feedback", "id = ?", missingId);

        given()
                .header("Authorization", "Bearer " + user.token)
                .pathParam("id", missingId)
        .when()
                .get("/api/v1/feedback/my/{id}")
        .then()
                .statusCode(404);
    }

    /**
     * Purpose: ensure malformed id path parameter triggers 400 with error body.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my/{id} returns 400 for invalid id format")
    void shouldReturn400WhenDetailRequestedWithInvalidIdFormat() {
        TestUser user = registerUser("feedback_detail_invalid_id");

        given()
                .header("Authorization", "Bearer " + user.token)
                .pathParam("id", "abc123")
        .when()
                .get("/api/v1/feedback/my/{id}")
        .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("code", equalTo(1001))
                .body("message", containsString("abc123"));
    }

    /**
     * Purpose: ensure requesting detail without token returns 401.
     */
    @Test
    @DisplayName("GET /api/v1/feedback/my/{id} returns 401 without token")
    void shouldReturn401WhenDetailRequestedWithoutToken() {
        TestUser user = registerUser("feedback_detail_noauth");
        Long feedbackId = submitFeedback(user, "bug", "No auth detail", uniqueSuffix("detail_noauth") + "@example.com", "/detail", "web");

        given()
                .pathParam("id", feedbackId)
        .when()
                .get("/api/v1/feedback/my/{id}")
        .then()
                .statusCode(401)
                .body("error", equalTo("未授权访问"))
                .body("message", equalTo("请先登录"));
    }

    private Long submitFeedback(TestUser user, String type, String content, String contact, String pageRoute, String scene) {
        JSONObject payload = new JSONObject();
        payload.put("type", type);
        payload.put("content", content);
        payload.put("contact", contact);
        payload.put("images", List.of("https://cdn.example.com/" + uniqueSuffix("img")));
        payload.put("appVersion", "2.0.0");
        payload.put("osVersion", "iOS 17.0");
        payload.put("deviceModel", "iPhone16,2");
        payload.put("networkType", "5g");
        payload.put("pageRoute", pageRoute);
        payload.put("extraData", Map.of("scene", scene, "channel", "app"));

        String response =
                given()
                        .header("Authorization", "Bearer " + user.token)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "JUnit-Agent/1.0")
                        .body(payload.toJSONString())
                .when()
                        .post("/api/v1/feedback/submit")
                .then()
                        .statusCode(200)
                        .body("id", notNullValue())
                        .extract()
                        .asString();

        return JSONObject.parseObject(response).getLong("id");
    }

    private TestUser registerUser(String prefix) {
        String suffix = uniqueSuffix(prefix);
        String email = suffix + "@example.com";
        String username = suffix;
        String password = "P@ssw0rd" + ThreadLocalRandom.current().nextInt(10, 99);

        JSONObject payload = new JSONObject();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("password", password);

        String response =
                given()
                        .header("Content-Type", "application/json")
                        .body(payload.toJSONString())
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
                user.token = json.getString("accessToken");
        return user;
    }

    private String uniqueSuffix(String prefix) {
        long now = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "_" + now + "_" + rand;
    }

        private static class TestUser {
                Long id;
                String token;
    }
}
