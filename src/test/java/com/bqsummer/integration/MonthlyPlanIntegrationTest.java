package com.bqsummer.integration;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.character.MonthlyPlan;
import com.bqsummer.mapper.MonthlyPlanMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 月度计划集成测试
 * <p>
 * 端到端测试月度计划的 CRUD 功能
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("月度计划集成测试")
class MonthlyPlanIntegrationTest extends BaseTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MonthlyPlanMapper monthlyPlanMapper;

    private Long testCharacterId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        // 创建一个测试用的 AI 角色
        testCharacterId = createTestCharacter();
    }

    /**
     * 创建测试用的 AI 角色
     */
    private Long createTestCharacter() {
        String requestBody = """
                {
                    "name": "月度计划测试AI角色",
                    "imageUrl": "https://example.com/test-avatar.jpg",
                    "visibility": "PUBLIC",
                    "status": 1
                }
                """;

        Integer characterIdInt = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/ai/characters")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        return characterIdInt.longValue();
    }

    // ========================================
    // US1: 创建月度计划测试
    // ========================================

    @Nested
    @DisplayName("US1: 创建月度计划")
    class CreateMonthlyPlanTests {

        /**
         * T010 - 场景1：成功创建固定日期计划
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("成功创建固定日期计划（day=5）")
        void testCreatePlanWithFixedDayRule() {
            // Given: 准备创建计划的请求数据
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=5",
                        "startTime": "09:00",
                        "durationMin": 60,
                        "location": "办公室",
                        "action": "晨会",
                        "participants": "[\\"张三\\", \\"李四\\"]",
                        "extra": "{\\"priority\\": \\"high\\"}"
                    }
                    """.formatted(testCharacterId);

            // When: 调用创建计划 API
            Integer planIdInt = given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("characterId", equalTo(testCharacterId.intValue()))
                    .body("dayRule", equalTo("day=5"))
                    .body("durationMin", equalTo(60))
                    .body("location", equalTo("办公室"))
                    .body("action", equalTo("晨会"))
                    .extract()
                    .path("id");

            // Then: 验证数据已持久化
            assertNotNull(planIdInt, "应该返回计划 ID");
            Long planId = planIdInt.longValue();

            MonthlyPlan plan = monthlyPlanMapper.findById(planId);
            assertNotNull(plan, "计划应该已创建");
            assertEquals(testCharacterId, plan.getCharacterId(), "虚拟人物 ID 应该正确");
            assertEquals("day=5", plan.getDayRule(), "日期规则应该正确");
        }

        /**
         * T010 - 场景2：成功创建相对日期计划
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("成功创建相对日期计划（weekday=1,week=2）")
        void testCreatePlanWithRelativeDayRule() {
            // Given: 准备创建计划的请求数据（每月第2个周一）
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "weekday=1,week=2",
                        "startTime": "14:00:00",
                        "durationMin": 120,
                        "action": "月度总结会议"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 调用创建计划 API
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("dayRule", equalTo("weekday=1,week=2"))
                    .body("durationMin", equalTo(120))
                    .body("action", equalTo("月度总结会议"));
        }

        /**
         * T010 - 场景3：虚拟人物不存在时返回 404
         */
        @Test
        @DisplayName("虚拟人物不存在时返回 404")
        void testCreatePlanWithNonExistentCharacter() {
            // Given: 使用不存在的虚拟人物 ID
            String requestBody = """
                    {
                        "characterId": 999999,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动"
                    }
                    """;

            // When & Then: 应该返回 404
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(404);
        }

        /**
         * T010 - 场景4：非创建者操作时返回 403
         */
        @Test
        @DisplayName("非创建者操作时返回 403")
        void testCreatePlanByNonCreator() {
            // Given: 注册另一个用户
            String anotherUserEmail = "another_plan_user_" + System.currentTimeMillis() + "@example.com";
            String anotherUserToken = given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "username": "another_plan_user_%s",
                                "email": "%s",
                                "password": "password123",
                                "phone": "13900000002"
                            }
                            """.formatted(System.currentTimeMillis(), anotherUserEmail))
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getString("accessToken");

            // Given: 准备创建计划的请求数据
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 非创建者应该返回 403
            given()
                    .header("Authorization", "Bearer " + anotherUserToken)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(403);
        }

        /**
         * T010 - 场景5：日期规则格式错误时返回 400
         */
        @Test
        @DisplayName("日期规则格式错误时返回 400")
        void testCreatePlanWithInvalidDayRule() {
            // Given: 使用无效的日期规则格式
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "invalid_rule",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400)
                    .body(containsString("日期规则格式不正确"));
        }

        /**
         * T010 - 附加：day 值超出范围时返回 400
         */
        @Test
        @DisplayName("day 值超出范围时返回 400")
        void testCreatePlanWithDayOutOfRange() {
            // Given: day=32 超出有效范围
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=32",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400);
        }

        /**
         * T010 - 附加：持续时间为 0 时返回 400
         */
        @Test
        @DisplayName("持续时间为 0 时返回 400")
        void testCreatePlanWithZeroDuration() {
            // Given: durationMin = 0
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 0,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400)
                    .body(containsString("持续时间必须大于0"));
        }

        /**
         * T010 - 附加：时间格式错误时返回 400
         */
        @Test
        @DisplayName("时间格式错误时返回 400")
        void testCreatePlanWithInvalidTimeFormat() {
            // Given: 无效的时间格式
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "25:00",
                        "durationMin": 30,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400)
                    .body(containsString("时间格式不正确"));
        }

        /**
         * T010 - 附加：participants 不是 JSON 数组时返回 400
         */
        @Test
        @DisplayName("participants 不是 JSON 数组时返回 400")
        void testCreatePlanWithInvalidParticipantsFormat() {
            // Given: participants 不是 JSON 数组
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动",
                        "participants": "{\\"name\\": \\"test\\"}"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400)
                    .body(containsString("参与者格式不正确"));
        }

        /**
         * T010 - 附加：extra 不是 JSON 对象时返回 400
         */
        @Test
        @DisplayName("extra 不是 JSON 对象时返回 400")
        void testCreatePlanWithInvalidExtraFormat() {
            // Given: extra 不是 JSON 对象
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 30,
                        "action": "测试活动",
                        "extra": "[\\"item1\\", \\"item2\\"]"
                    }
                    """.formatted(testCharacterId);

            // When & Then: 应该返回 400
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(400)
                    .body(containsString("扩展信息格式不正确"));
        }
    }

    // ========================================
    // US2: 查询月度计划测试
    // ========================================

    @Nested
    @DisplayName("US2: 查询月度计划")
    class QueryMonthlyPlanTests {

        /**
         * T016 - 场景1：查询有多条计划的列表
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("查询有多条计划的列表")
        void testListMultiplePlans() {
            // Given: 创建多条计划
            createTestPlan("day=1", "09:00", 60, "早会");
            createTestPlan("day=15", "14:00", 120, "月中总结");
            createTestPlan("weekday=5,week=4", "17:00", 90, "月末聚餐");

            // When & Then: 查询计划列表
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/ai/characters/" + testCharacterId + "/plans")
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThanOrEqualTo(3));
        }

        /**
         * T016 - 场景2：查询空列表
         */
        @Test
        @DisplayName("查询空列表")
        void testListEmptyPlans() {
            // Given: 创建新的 AI 角色（没有计划）
            Long newCharacterId = createTestCharacter();

            // When & Then: 查询计划列表应该为空
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/ai/characters/" + newCharacterId + "/plans")
                    .then()
                    .statusCode(200)
                    .body("size()", equalTo(0));
        }

        /**
         * T016 - 场景3：查询单条计划详情成功
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("查询单条计划详情成功")
        void testGetPlanById() {
            // Given: 创建一条计划
            Long planId = createTestPlan("day=10", "10:30", 45, "技术分享");

            // When & Then: 查询计划详情
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(planId.intValue()))
                    .body("dayRule", equalTo("day=10"))
                    .body("action", equalTo("技术分享"));
        }

        /**
         * T016 - 场景4：查询不存在的计划返回 404
         */
        @Test
        @DisplayName("查询不存在的计划返回 404")
        void testGetNonExistentPlan() {
            // When & Then: 查询不存在的计划
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/ai/characters/plans/999999")
                    .then()
                    .statusCode(404);
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan(String dayRule, String startTime, int durationMin, String action) {
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "%s",
                        "startTime": "%s",
                        "durationMin": %d,
                        "action": "%s"
                    }
                    """.formatted(testCharacterId, dayRule, startTime, durationMin, action);

            Integer planIdInt = given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            return planIdInt.longValue();
        }
    }

    // ========================================
    // US3: 更新月度计划测试
    // ========================================

    @Nested
    @DisplayName("US3: 更新月度计划")
    class UpdateMonthlyPlanTests {

        /**
         * T022 - 场景1：成功更新部分字段
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("成功更新部分字段")
        void testUpdatePartialFields() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When: 更新部分字段
            String updateBody = """
                    {
                        "action": "更新后的活动",
                        "durationMin": 90
                    }
                    """;

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(updateBody)
                    .when()
                    .put("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(200);

            // Then: 验证更新成功
            MonthlyPlan plan = monthlyPlanMapper.findById(planId);
            assertEquals("更新后的活动", plan.getAction());
            assertEquals(90, plan.getDurationMin());
        }

        /**
         * T022 - 场景2：更新不存在的计划返回 404
         */
        @Test
        @DisplayName("更新不存在的计划返回 404")
        void testUpdateNonExistentPlan() {
            String updateBody = """
                    {
                        "action": "更新后的活动"
                    }
                    """;

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(updateBody)
                    .when()
                    .put("/api/v1/ai/characters/plans/999999")
                    .then()
                    .statusCode(404);
        }

        /**
         * T022 - 场景3：非创建者更新返回 403
         */
        @Test
        @DisplayName("非创建者更新返回 403")
        void testUpdateByNonCreator() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // Given: 注册另一个用户
            String anotherUserEmail = "another_update_user_" + System.currentTimeMillis() + "@example.com";
            String anotherUserToken = given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "username": "another_update_user_%s",
                                "email": "%s",
                                "password": "password123",
                                "phone": "13900000003"
                            }
                            """.formatted(System.currentTimeMillis(), anotherUserEmail))
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getString("accessToken");

            // When & Then: 非创建者更新应该返回 403
            String updateBody = """
                    {
                        "action": "更新后的活动"
                    }
                    """;

            given()
                    .header("Authorization", "Bearer " + anotherUserToken)
                    .contentType(ContentType.JSON)
                    .body(updateBody)
                    .when()
                    .put("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(403);
        }

        /**
         * T022 - 场景4：更新日期规则格式错误返回 400
         */
        @Test
        @DisplayName("更新日期规则格式错误返回 400")
        void testUpdateWithInvalidDayRule() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When & Then: 使用无效的日期规则格式
            String updateBody = """
                    {
                        "dayRule": "invalid_rule"
                    }
                    """;

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(updateBody)
                    .when()
                    .put("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(400);
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan() {
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 60,
                        "action": "测试活动"
                    }
                    """.formatted(testCharacterId);

            Integer planIdInt = given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            return planIdInt.longValue();
        }
    }

    // ========================================
    // US4: 删除月度计划测试
    // ========================================

    @Nested
    @DisplayName("US4: 删除月度计划")
    class DeleteMonthlyPlanTests {

        /**
         * T028 - 场景1：成功删除计划
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("成功删除计划")
        void testDeletePlan() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When: 删除计划
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(200);

            // Then: 验证计划已被软删除
            MonthlyPlan plan = monthlyPlanMapper.findById(planId);
            assertNull(plan, "软删除后应该查不到计划");
        }

        /**
         * T028 - 场景2：删除后查询返回空/不包含该计划
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("删除后查询不包含该计划")
        void testDeletedPlanNotInList() {
            // Given: 创建两条计划
            Long planId1 = createTestPlan("测试活动1");
            Long planId2 = createTestPlan("测试活动2");

            // When: 删除第一条计划
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete("/api/v1/ai/characters/plans/" + planId1)
                    .then()
                    .statusCode(200);

            // Then: 查询列表不应包含已删除的计划
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/v1/ai/characters/" + testCharacterId + "/plans")
                    .then()
                    .statusCode(200)
                    .body("find { it.id == " + planId1 + " }", nullValue())
                    .body("find { it.id == " + planId2 + " }", notNullValue());
        }

        /**
         * T028 - 场景3：删除不存在的计划返回 404
         */
        @Test
        @DisplayName("删除不存在的计划返回 404")
        void testDeleteNonExistentPlan() {
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .delete("/api/v1/ai/characters/plans/999999")
                    .then()
                    .statusCode(404);
        }

        /**
         * T028 - 场景4：非创建者删除返回 403
         */
        @Test
        @DisplayName("非创建者删除返回 403")
        void testDeleteByNonCreator() {
            // Given: 创建一条计划
            Long planId = createTestPlan("测试活动");

            // Given: 注册另一个用户
            String anotherUserEmail = "another_delete_user_" + System.currentTimeMillis() + "@example.com";
            String anotherUserToken = given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "username": "another_delete_user_%s",
                                "email": "%s",
                                "password": "password123",
                                "phone": "13900000004"
                            }
                            """.formatted(System.currentTimeMillis(), anotherUserEmail))
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getString("accessToken");

            // When & Then: 非创建者删除应该返回 403
            given()
                    .header("Authorization", "Bearer " + anotherUserToken)
                    .when()
                    .delete("/api/v1/ai/characters/plans/" + planId)
                    .then()
                    .statusCode(403);
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan() {
            return createTestPlan("测试活动");
        }

        private Long createTestPlan(String action) {
            String requestBody = """
                    {
                        "characterId": %d,
                        "dayRule": "day=1",
                        "startTime": "10:00",
                        "durationMin": 60,
                        "action": "%s"
                    }
                    """.formatted(testCharacterId, action);

            Integer planIdInt = given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/api/v1/ai/characters/plans")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            return planIdInt.longValue();
        }
    }
}
