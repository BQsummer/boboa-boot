package com.bqsummer.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.application.name=boboa-boot", "spring.profiles.active=default"}
)
@DisplayName("人物日程状态接口集成测试")
@Disabled("当前测试环境缺少 AiModelStructMapper Bean，应用上下文无法启动")
class CharacterScheduleStateIntegrationTest {

    @LocalServerPort
    private int port;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        token = loginAsAdmin();
        Assertions.assertNotNull(token);
    }

    private String loginAsAdmin() {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "usernameOrEmail": "admin",
                          "password": "admin123"
                        }
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("特殊事件优先覆盖循环规则")
    void shouldReturnSpecialEventWhenEventExists() {
        String createRuleBody = """
                {
                  "characterKey": "alice-test",
                  "title": "工作日白天在公司",
                  "recurrenceType": "WEEKLY",
                  "priority": 10,
                  "patterns": [{"weekdayMask": 31}],
                  "slots": [{
                    "startTime": "09:00:00",
                    "endTime": "18:00:00",
                    "locationText": "公司-会议室A",
                    "activityText": "开会"
                  }]
                }
                """;

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createRuleBody)
                .when()
                .post("/api/v1/ai/characters/schedules/rules")
                .then()
                .statusCode(200)
                .body("id", notNullValue());

        String createEventBody = """
                {
                  "characterKey": "alice-test",
                  "title": "临时发布会",
                  "startAt": "2026-02-16T10:00:00+08:00",
                  "endAt": "2026-02-16T12:00:00+08:00",
                  "locationText": "酒店-宴会厅",
                  "activityText": "参加发布会",
                  "overrideMode": "REPLACE",
                  "priority": 100
                }
                """;

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createEventBody)
                .when()
                .post("/api/v1/ai/characters/schedules/events")
                .then()
                .statusCode(200)
                .body("id", notNullValue());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/ai/characters/state?characterKey=alice-test&t=2026-02-16T10:30:00+08:00")
                .then()
                .statusCode(200)
                .body("characterKey", equalTo("alice-test"))
                .body("locationText", equalTo("酒店-宴会厅"))
                .body("activityText", equalTo("参加发布会"))
                .body("source.type", equalTo("EVENT"));
    }
}
