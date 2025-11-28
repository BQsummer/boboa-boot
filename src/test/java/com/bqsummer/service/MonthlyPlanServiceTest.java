package com.bqsummer.service;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.character.AiCharacter;
import com.bqsummer.common.dto.character.MonthlyPlan;
import com.bqsummer.common.vo.req.chararcter.CreateMonthlyPlanReq;
import com.bqsummer.common.vo.req.chararcter.UpdateMonthlyPlanReq;
import com.bqsummer.mapper.AiCharacterMapper;
import com.bqsummer.mapper.MonthlyPlanMapper;
import com.bqsummer.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 月度计划服务单元测试
 * <p>
 * 测试 MonthlyPlanService 的业务逻辑
 * </p>
 */
@SpringBootTest
@DisplayName("月度计划服务单元测试")
class MonthlyPlanServiceTest extends BaseTest {

    @Autowired
    private MonthlyPlanService monthlyPlanService;

    @Autowired
    private MonthlyPlanMapper monthlyPlanMapper;

    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    @Autowired
    private UserMapper userMapper;

    private User testCreator;
    private AiCharacter testCharacter;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testCreator = User.builder()
                .username("test_creator_" + System.currentTimeMillis())
                .email("creator_" + System.currentTimeMillis() + "@test.com")
                .password("test123")
                .nickName("测试创建者")
                .status(1)
                .build();
        userMapper.insert(testCreator);

        // 创建测试 AI 角色
        testCharacter = AiCharacter.builder()
                .name("测试AI角色")
                .imageUrl("https://example.com/test.jpg")
                .author("测试作者")
                .visibility("PUBLIC")
                .status(1)
                .createdByUserId(testCreator.getId())
                .isDeleted(0)
                .build();
        aiCharacterMapper.insert(testCharacter);
    }

    // ========================================
    // US1: 创建月度计划测试
    // ========================================

    @Nested
    @DisplayName("US1: 创建月度计划")
    class CreateMonthlyPlanTests {

        /**
         * T011 - 测试固定日期规则验证：day=N，N ∈ [1, 31]
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("固定日期规则验证：day=N 应该接受 N ∈ [1, 31]")
        void testFixedDayRuleValidation() {
            // Given: 准备有效的固定日期规则
            String[] validRules = {"day=1", "day=15", "day=31"};

            for (String rule : validRules) {
                CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
                req.setCharacterId(testCharacter.getId());
                req.setDayRule(rule);
                req.setStartTime("10:00");
                req.setDurationMin(60);
                req.setAction("测试活动");

                // When: 创建计划
                ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());

                // Then: 应该成功
                assertEquals(200, response.getStatusCode().value(),
                        "规则 " + rule + " 应该是有效的");
            }
        }

        /**
         * T011 - 测试固定日期规则验证：day 值超出范围应失败
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("固定日期规则验证：day=0 或 day=32 应该失败")
        void testFixedDayRuleValidationOutOfRange() {
            // Given: 准备无效的固定日期规则
            String[] invalidRules = {"day=0", "day=32", "day=-1", "day=100"};

            for (String rule : invalidRules) {
                CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
                req.setCharacterId(testCharacter.getId());
                req.setDayRule(rule);
                req.setStartTime("10:00");
                req.setDurationMin(60);
                req.setAction("测试活动");

                // When: 创建计划
                ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());

                // Then: 应该失败
                assertEquals(400, response.getStatusCode().value(),
                        "规则 " + rule + " 应该是无效的");
            }
        }

        /**
         * T011 - 测试相对日期规则验证：weekday=W,week=K
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("相对日期规则验证：weekday=W,week=K 应该接受 W ∈ [1,7], K ∈ [1,5]")
        void testRelativeDayRuleValidation() {
            // Given: 准备有效的相对日期规则
            String[] validRules = {
                    "weekday=1,week=1",  // 每月第1个周一
                    "weekday=7,week=5",  // 每月第5个周日
                    "weekday=3,week=2"   // 每月第2个周三
            };

            for (String rule : validRules) {
                CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
                req.setCharacterId(testCharacter.getId());
                req.setDayRule(rule);
                req.setStartTime("10:00");
                req.setDurationMin(60);
                req.setAction("测试活动");

                // When: 创建计划
                ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());

                // Then: 应该成功
                assertEquals(200, response.getStatusCode().value(),
                        "规则 " + rule + " 应该是有效的");
            }
        }

        /**
         * T011 - 测试相对日期规则验证：weekday 或 week 超出范围应失败
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("相对日期规则验证：超出范围应该失败")
        void testRelativeDayRuleValidationOutOfRange() {
            // Given: 准备无效的相对日期规则
            String[] invalidRules = {
                    "weekday=0,week=1",   // weekday 超出范围
                    "weekday=8,week=1",   // weekday 超出范围
                    "weekday=1,week=0",   // week 超出范围
                    "weekday=1,week=6"    // week 超出范围
            };

            for (String rule : invalidRules) {
                CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
                req.setCharacterId(testCharacter.getId());
                req.setDayRule(rule);
                req.setStartTime("10:00");
                req.setDurationMin(60);
                req.setAction("测试活动");

                // When: 创建计划
                ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());

                // Then: 应该失败
                assertEquals(400, response.getStatusCode().value(),
                        "规则 " + rule + " 应该是无效的");
            }
        }

        /**
         * T011 - 测试 JSON 数组字段验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("participants 必须是有效的 JSON 数组")
        void testParticipantsJsonArrayValidation() {
            // Given: 准备有效的 JSON 数组
            CreateMonthlyPlanReq validReq = new CreateMonthlyPlanReq();
            validReq.setCharacterId(testCharacter.getId());
            validReq.setDayRule("day=1");
            validReq.setStartTime("10:00");
            validReq.setDurationMin(60);
            validReq.setAction("测试活动");
            validReq.setParticipants("[\"张三\", \"李四\"]");

            // When: 创建计划
            ResponseEntity<?> validResponse = monthlyPlanService.createPlan(validReq, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, validResponse.getStatusCode().value(),
                    "有效的 JSON 数组应该被接受");

            // Given: 准备无效的 JSON 数组（实际是对象）
            CreateMonthlyPlanReq invalidReq = new CreateMonthlyPlanReq();
            invalidReq.setCharacterId(testCharacter.getId());
            invalidReq.setDayRule("day=2");
            invalidReq.setStartTime("10:00");
            invalidReq.setDurationMin(60);
            invalidReq.setAction("测试活动");
            invalidReq.setParticipants("{\"name\": \"test\"}");

            // When: 创建计划
            ResponseEntity<?> invalidResponse = monthlyPlanService.createPlan(invalidReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, invalidResponse.getStatusCode().value(),
                    "JSON 对象不应该被接受为 participants");
        }

        /**
         * T011 - 测试 JSON 对象字段验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("extra 必须是有效的 JSON 对象")
        void testExtraJsonObjectValidation() {
            // Given: 准备有效的 JSON 对象
            CreateMonthlyPlanReq validReq = new CreateMonthlyPlanReq();
            validReq.setCharacterId(testCharacter.getId());
            validReq.setDayRule("day=1");
            validReq.setStartTime("10:00");
            validReq.setDurationMin(60);
            validReq.setAction("测试活动");
            validReq.setExtra("{\"priority\": \"high\"}");

            // When: 创建计划
            ResponseEntity<?> validResponse = monthlyPlanService.createPlan(validReq, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, validResponse.getStatusCode().value(),
                    "有效的 JSON 对象应该被接受");

            // Given: 准备无效的 JSON 对象（实际是数组）
            CreateMonthlyPlanReq invalidReq = new CreateMonthlyPlanReq();
            invalidReq.setCharacterId(testCharacter.getId());
            invalidReq.setDayRule("day=2");
            invalidReq.setStartTime("10:00");
            invalidReq.setDurationMin(60);
            invalidReq.setAction("测试活动");
            invalidReq.setExtra("[\"item1\", \"item2\"]");

            // When: 创建计划
            ResponseEntity<?> invalidResponse = monthlyPlanService.createPlan(invalidReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, invalidResponse.getStatusCode().value(),
                    "JSON 数组不应该被接受为 extra");
        }

        /**
         * T011 - 测试持续时间验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("持续时间必须大于 0")
        void testDurationMinValidation() {
            // Given: 准备持续时间为 0 的请求
            CreateMonthlyPlanReq zeroReq = new CreateMonthlyPlanReq();
            zeroReq.setCharacterId(testCharacter.getId());
            zeroReq.setDayRule("day=1");
            zeroReq.setStartTime("10:00");
            zeroReq.setDurationMin(0);
            zeroReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> zeroResponse = monthlyPlanService.createPlan(zeroReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, zeroResponse.getStatusCode().value(),
                    "持续时间为 0 应该失败");

            // Given: 准备持续时间为负数的请求
            CreateMonthlyPlanReq negativeReq = new CreateMonthlyPlanReq();
            negativeReq.setCharacterId(testCharacter.getId());
            negativeReq.setDayRule("day=2");
            negativeReq.setStartTime("10:00");
            negativeReq.setDurationMin(-10);
            negativeReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> negativeResponse = monthlyPlanService.createPlan(negativeReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, negativeResponse.getStatusCode().value(),
                    "持续时间为负数应该失败");
        }

        /**
         * T011 - 测试时间格式验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("时间格式验证：支持 HH:mm 和 HH:mm:ss")
        void testTimeFormatValidation() {
            // Given: 准备 HH:mm 格式的时间
            CreateMonthlyPlanReq shortTimeReq = new CreateMonthlyPlanReq();
            shortTimeReq.setCharacterId(testCharacter.getId());
            shortTimeReq.setDayRule("day=1");
            shortTimeReq.setStartTime("10:30");
            shortTimeReq.setDurationMin(60);
            shortTimeReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> shortTimeResponse = monthlyPlanService.createPlan(shortTimeReq, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, shortTimeResponse.getStatusCode().value(),
                    "HH:mm 格式应该被接受");

            // Given: 准备 HH:mm:ss 格式的时间
            CreateMonthlyPlanReq longTimeReq = new CreateMonthlyPlanReq();
            longTimeReq.setCharacterId(testCharacter.getId());
            longTimeReq.setDayRule("day=2");
            longTimeReq.setStartTime("14:30:45");
            longTimeReq.setDurationMin(60);
            longTimeReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> longTimeResponse = monthlyPlanService.createPlan(longTimeReq, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, longTimeResponse.getStatusCode().value(),
                    "HH:mm:ss 格式应该被接受");

            // Given: 准备无效的时间格式
            CreateMonthlyPlanReq invalidTimeReq = new CreateMonthlyPlanReq();
            invalidTimeReq.setCharacterId(testCharacter.getId());
            invalidTimeReq.setDayRule("day=3");
            invalidTimeReq.setStartTime("25:00");
            invalidTimeReq.setDurationMin(60);
            invalidTimeReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> invalidTimeResponse = monthlyPlanService.createPlan(invalidTimeReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, invalidTimeResponse.getStatusCode().value(),
                    "无效的时间格式应该失败");
        }

        /**
         * T011 - 测试必填字段验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("必填字段验证")
        void testRequiredFieldsValidation() {
            // Given: 缺少 action 的请求
            CreateMonthlyPlanReq noActionReq = new CreateMonthlyPlanReq();
            noActionReq.setCharacterId(testCharacter.getId());
            noActionReq.setDayRule("day=1");
            noActionReq.setStartTime("10:00");
            noActionReq.setDurationMin(60);
            // action 缺失

            // When: 创建计划
            ResponseEntity<?> noActionResponse = monthlyPlanService.createPlan(noActionReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, noActionResponse.getStatusCode().value(),
                    "缺少 action 应该失败");

            // Given: 缺少 dayRule 的请求
            CreateMonthlyPlanReq noDayRuleReq = new CreateMonthlyPlanReq();
            noDayRuleReq.setCharacterId(testCharacter.getId());
            // dayRule 缺失
            noDayRuleReq.setStartTime("10:00");
            noDayRuleReq.setDurationMin(60);
            noDayRuleReq.setAction("测试活动");

            // When: 创建计划
            ResponseEntity<?> noDayRuleResponse = monthlyPlanService.createPlan(noDayRuleReq, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, noDayRuleResponse.getStatusCode().value(),
                    "缺少 dayRule 应该失败");
        }

        /**
         * T011 - 测试权限验证：只有创建者可以管理计划
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("权限验证：只有虚拟人物的创建者可以创建计划")
        void testPermissionValidation() {
            // Given: 准备另一个用户
            User anotherUser = User.builder()
                    .username("another_user_" + System.currentTimeMillis())
                    .email("another_" + System.currentTimeMillis() + "@test.com")
                    .password("test123")
                    .nickName("另一个用户")
                    .status(1)
                    .build();
            userMapper.insert(anotherUser);

            // Given: 准备创建计划的请求
            CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
            req.setCharacterId(testCharacter.getId());
            req.setDayRule("day=1");
            req.setStartTime("10:00");
            req.setDurationMin(60);
            req.setAction("测试活动");

            // When: 非创建者尝试创建计划
            ResponseEntity<?> response = monthlyPlanService.createPlan(req, anotherUser.getId());

            // Then: 应该返回 403
            assertEquals(403, response.getStatusCode().value(),
                    "非创建者不应该能够创建计划");
        }
    }

    // ========================================
    // US2: 查询月度计划测试
    // ========================================

    @Nested
    @DisplayName("US2: 查询月度计划")
    class QueryMonthlyPlanTests {

        /**
         * T017 - 测试查询计划列表
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("查询虚拟人物的计划列表")
        void testListByCharacterId() {
            // Given: 创建两条计划
            createTestPlan("day=1", "测试活动1");
            createTestPlan("day=15", "测试活动2");

            // When: 查询计划列表
            ResponseEntity<?> response = monthlyPlanService.listPlansByCharacterId(
                    testCharacter.getId(), testCreator.getId());

            // Then: 应该成功且包含两条计划
            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody() instanceof java.util.List);
            @SuppressWarnings("unchecked")
            java.util.List<MonthlyPlan> plans = (java.util.List<MonthlyPlan>) response.getBody();
            assertTrue(plans.size() >= 2, "应该至少有两条计划");
        }

        /**
         * T017 - 测试查询空列表
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("查询没有计划的虚拟人物返回空列表")
        void testListEmptyPlans() {
            // Given: 创建一个新的 AI 角色（没有计划）
            AiCharacter newCharacter = AiCharacter.builder()
                    .name("新AI角色")
                    .imageUrl("https://example.com/new.jpg")
                    .visibility("PUBLIC")
                    .status(1)
                    .createdByUserId(testCreator.getId())
                    .isDeleted(0)
                    .build();
            aiCharacterMapper.insert(newCharacter);

            // When: 查询计划列表
            ResponseEntity<?> response = monthlyPlanService.listPlansByCharacterId(
                    newCharacter.getId(), testCreator.getId());

            // Then: 应该成功且返回空列表
            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody() instanceof java.util.List);
            @SuppressWarnings("unchecked")
            java.util.List<MonthlyPlan> plans = (java.util.List<MonthlyPlan>) response.getBody();
            assertEquals(0, plans.size(), "应该返回空列表");
        }

        /**
         * T017 - 测试查询单条计划详情
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("查询单条计划详情")
        void testGetById() {
            // Given: 创建一条计划
            Long planId = createTestPlan("day=10", "技术分享");

            // When: 查询计划详情
            ResponseEntity<?> response = monthlyPlanService.getPlanById(planId, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, response.getStatusCode().value());
            assertTrue(response.getBody() instanceof MonthlyPlan);
            MonthlyPlan plan = (MonthlyPlan) response.getBody();
            assertEquals(planId, plan.getId());
            assertEquals("day=10", plan.getDayRule());
            assertEquals("技术分享", plan.getAction());
        }

        /**
         * T017 - 测试查询不存在的计划返回 404
         */
        @Test
        @DisplayName("查询不存在的计划返回 404")
        void testGetByIdNotFound() {
            // When: 查询不存在的计划
            ResponseEntity<?> response = monthlyPlanService.getPlanById(999999L, testCreator.getId());

            // Then: 应该返回 404
            assertEquals(404, response.getStatusCode().value());
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan(String dayRule, String action) {
            CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
            req.setCharacterId(testCharacter.getId());
            req.setDayRule(dayRule);
            req.setStartTime("10:00");
            req.setDurationMin(60);
            req.setAction(action);

            ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());
            assertEquals(200, response.getStatusCode().value());
            MonthlyPlan plan = (MonthlyPlan) response.getBody();
            return plan.getId();
        }
    }

    // ========================================
    // US3: 更新月度计划测试
    // ========================================

    @Nested
    @DisplayName("US3: 更新月度计划")
    class UpdateMonthlyPlanTests {

        /**
         * T023 - 测试部分更新
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("部分更新只修改提供的字段")
        void testPartialUpdate() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When: 只更新 action 字段
            UpdateMonthlyPlanReq req = new UpdateMonthlyPlanReq();
            req.setAction("更新后的活动");

            ResponseEntity<?> response = monthlyPlanService.updatePlan(planId, req, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, response.getStatusCode().value());

            // 验证只有 action 被更新
            MonthlyPlan plan = monthlyPlanMapper.findById(planId);
            assertEquals("更新后的活动", plan.getAction());
            assertEquals("day=1", plan.getDayRule(), "未提供的字段应该保持不变");
            assertEquals(60, plan.getDurationMin(), "未提供的字段应该保持不变");
        }

        /**
         * T023 - 测试更新日期规则验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("更新时日期规则格式错误应失败")
        void testUpdateWithInvalidDayRule() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When: 使用无效的日期规则
            UpdateMonthlyPlanReq req = new UpdateMonthlyPlanReq();
            req.setDayRule("invalid_rule");

            ResponseEntity<?> response = monthlyPlanService.updatePlan(planId, req, testCreator.getId());

            // Then: 应该失败
            assertEquals(400, response.getStatusCode().value());
        }

        /**
         * T023 - 测试更新权限验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("非创建者不能更新计划")
        void testUpdatePermission() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // Given: 准备另一个用户
            User anotherUser = User.builder()
                    .username("another_update_user_" + System.currentTimeMillis())
                    .email("another_update_" + System.currentTimeMillis() + "@test.com")
                    .password("test123")
                    .nickName("另一个用户")
                    .status(1)
                    .build();
            userMapper.insert(anotherUser);

            // When: 非创建者尝试更新
            UpdateMonthlyPlanReq req = new UpdateMonthlyPlanReq();
            req.setAction("更新后的活动");

            ResponseEntity<?> response = monthlyPlanService.updatePlan(planId, req, anotherUser.getId());

            // Then: 应该返回 403
            assertEquals(403, response.getStatusCode().value());
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan() {
            CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
            req.setCharacterId(testCharacter.getId());
            req.setDayRule("day=1");
            req.setStartTime("10:00");
            req.setDurationMin(60);
            req.setAction("测试活动");

            ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());
            assertEquals(200, response.getStatusCode().value());
            MonthlyPlan plan = (MonthlyPlan) response.getBody();
            return plan.getId();
        }
    }

    // ========================================
    // US4: 删除月度计划测试
    // ========================================

    @Nested
    @DisplayName("US4: 删除月度计划")
    class DeleteMonthlyPlanTests {

        /**
         * T029 - 测试软删除
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("删除后计划应该无法查询")
        void testSoftDelete() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // When: 删除计划
            ResponseEntity<?> deleteResponse = monthlyPlanService.deletePlan(planId, testCreator.getId());

            // Then: 应该成功
            assertEquals(200, deleteResponse.getStatusCode().value());

            // 验证无法查询
            MonthlyPlan plan = monthlyPlanMapper.findById(planId);
            assertNull(plan, "软删除后应该查不到计划");
        }

        /**
         * T029 - 测试删除不存在的计划返回 404
         */
        @Test
        @DisplayName("删除不存在的计划返回 404")
        void testDeleteNotFound() {
            // When: 删除不存在的计划
            ResponseEntity<?> response = monthlyPlanService.deletePlan(999999L, testCreator.getId());

            // Then: 应该返回 404
            assertEquals(404, response.getStatusCode().value());
        }

        /**
         * T029 - 测试删除权限验证
         */
        @Test
        @Transactional
        @Rollback
        @DisplayName("非创建者不能删除计划")
        void testDeletePermission() {
            // Given: 创建一条计划
            Long planId = createTestPlan();

            // Given: 准备另一个用户
            User anotherUser = User.builder()
                    .username("another_delete_user_" + System.currentTimeMillis())
                    .email("another_delete_" + System.currentTimeMillis() + "@test.com")
                    .password("test123")
                    .nickName("另一个用户")
                    .status(1)
                    .build();
            userMapper.insert(anotherUser);

            // When: 非创建者尝试删除
            ResponseEntity<?> response = monthlyPlanService.deletePlan(planId, anotherUser.getId());

            // Then: 应该返回 403
            assertEquals(403, response.getStatusCode().value());
        }

        /**
         * 创建测试计划的辅助方法
         */
        private Long createTestPlan() {
            CreateMonthlyPlanReq req = new CreateMonthlyPlanReq();
            req.setCharacterId(testCharacter.getId());
            req.setDayRule("day=1");
            req.setStartTime("10:00");
            req.setDurationMin(60);
            req.setAction("测试活动");

            ResponseEntity<?> response = monthlyPlanService.createPlan(req, testCreator.getId());
            assertEquals(200, response.getStatusCode().value());
            MonthlyPlan plan = (MonthlyPlan) response.getBody();
            return plan.getId();
        }
    }
}
