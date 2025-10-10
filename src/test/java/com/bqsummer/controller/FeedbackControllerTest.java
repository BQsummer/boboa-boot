package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.feedback.Feedback;
import com.bqsummer.common.vo.req.feedback.BatchUpdateStatusRequest;
import com.bqsummer.common.vo.req.feedback.SubmitFeedbackRequest;
import com.bqsummer.common.vo.req.feedback.UpdateFeedbackStatusRequest;
import com.bqsummer.service.FeedbackService;
import com.bqsummer.util.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(controllers = {FeedbackController.class, FeedbackAdminController.class})
@ContextConfiguration(classes = {FeedbackController.class, FeedbackAdminController.class})
@SuppressWarnings("deprecation") // 抑制MockBean废弃警告
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    @DisplayName("FeedbackController Tests")
    class FeedbackControllerTests {

        @Test
        @DisplayName("提交反馈 - 成功 - 无JWT Token")
        void submitFeedback_Success_NoToken() {
            // Given
            SubmitFeedbackRequest request = createValidSubmitRequest();
            when(feedbackService.submit(any(), anyString(), anyString())).thenReturn(123L);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("User-Agent", "TestAgent/1.0")
                .header("X-Forwarded-For", "192.168.1.100")
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(123));

            // Verify service call
            ArgumentCaptor<SubmitFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(SubmitFeedbackRequest.class);
            ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> uaCaptor = ArgumentCaptor.forClass(String.class);

            verify(feedbackService).submit(requestCaptor.capture(), ipCaptor.capture(), uaCaptor.capture());

            assertEquals("192.168.1.100", ipCaptor.getValue());
            assertEquals("TestAgent/1.0", uaCaptor.getValue());
        }

        @Test
        @DisplayName("提交反馈 - 成功 - 带JWT Token")
        void submitFeedback_Success_WithToken() {
            // Given
            SubmitFeedbackRequest request = createValidSubmitRequest();
            request.setUserId(999L); // 原始请求中的userId

            String token = "valid.jwt.token";
            when(JwtUtil.getUserIdFromToken(token)).thenReturn(456L);
            when(feedbackService.submit(any(), anyString(), anyString())).thenReturn(123L);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "TestAgent/1.0")
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(123));

            // Verify that JWT token userId overwrote the request userId
            ArgumentCaptor<SubmitFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(SubmitFeedbackRequest.class);
            verify(feedbackService).submit(requestCaptor.capture(), anyString(), anyString());

            assertEquals(456L, requestCaptor.getValue().getUserId());
        }

        @Test
        @DisplayName("提交反馈 - IP提取测试 - X-Real-IP优先于RemoteAddr")
        void submitFeedback_IPExtraction_XRealIP() {
            // Given
            SubmitFeedbackRequest request = createValidSubmitRequest();
            when(feedbackService.submit(any(), anyString(), anyString())).thenReturn(123L);

            // When & Then - 测试X-Real-IP头
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("X-Real-IP", "203.0.113.195")
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.OK.value());

            ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
            verify(feedbackService).submit(any(), ipCaptor.capture(), anyString());
            assertEquals("203.0.113.195", ipCaptor.getValue());
        }

        @Test
        @DisplayName("提交反馈 - IP提取测试 - X-Forwarded-For多IP")
        void submitFeedback_IPExtraction_XForwardedForMultiple() {
            // Given
            SubmitFeedbackRequest request = createValidSubmitRequest();
            when(feedbackService.submit(any(), anyString(), anyString())).thenReturn(123L);

            // When & Then - 测试X-Forwarded-For多IP情况
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("X-Forwarded-For", "203.0.113.195, 198.51.100.178, 192.0.2.146")
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.OK.value());

            ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);
            verify(feedbackService).submit(any(), ipCaptor.capture(), anyString());
            assertEquals("203.0.113.195", ipCaptor.getValue());
        }

        @Test
        @DisplayName("提交反馈 - 验证失败")
        void submitFeedback_ValidationFailure() {
            // Given - 空的type和content
            SubmitFeedbackRequest request = new SubmitFeedbackRequest();

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

            // Verify service was never called
            verify(feedbackService, never()).submit(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("提交反馈 - 无效Token时仍使用原始userId")
        void submitFeedback_InvalidToken_UseOriginalUserId() {
            // Given
            SubmitFeedbackRequest request = createValidSubmitRequest();
            request.setUserId(999L);

            String invalidToken = "invalid.jwt.token";
            when(JwtUtil.getUserIdFromToken(invalidToken)).thenThrow(new RuntimeException("Invalid token"));
            when(feedbackService.submit(any(), anyString(), anyString())).thenReturn(123L);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .header("Authorization", "Bearer " + invalidToken)
            .when()
                .post("/api/v1/feedback/submit")
            .then()
                .statusCode(HttpStatus.OK.value());

            // Verify original userId is preserved when token parsing fails
            ArgumentCaptor<SubmitFeedbackRequest> requestCaptor = ArgumentCaptor.forClass(SubmitFeedbackRequest.class);
            verify(feedbackService).submit(requestCaptor.capture(), anyString(), anyString());
            assertEquals(999L, requestCaptor.getValue().getUserId());
        }
    }

    @Nested
    @DisplayName("FeedbackAdminController Tests")
    class FeedbackAdminControllerTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取反馈列表 - 无搜索关键词")
        void adminListFeedback_NoKeyword() {
            // Given
            Page<Feedback> mockPage = createMockFeedbackPage();
            when(feedbackService.list("bug", "NEW", 123L, 1, 20)).thenReturn(mockPage);

            // When & Then
            given()
                .param("type", "bug")
                .param("status", "NEW")
                .param("userId", 123L)
                .param("page", 1)
                .param("size", 20)
            .when()
                .get("/api/v1/admin/feedback")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("records", hasSize(1))
                .body("records[0].id", equalTo(1))
                .body("records[0].type", equalTo("bug"))
                .body("total", equalTo(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取反馈列表 - 带搜索关键词")
        void adminListFeedback_WithKeyword() {
            // Given
            Page<Feedback> mockPage = createMockFeedbackPage();
            when(feedbackService.searchList("bug", "NEW", 123L, "crash", 1, 20)).thenReturn(mockPage);

            // When & Then
            given()
                .param("type", "bug")
                .param("status", "NEW")
                .param("userId", 123L)
                .param("keyword", "crash")
                .param("page", 1)
                .param("size", 20)
            .when()
                .get("/api/v1/admin/feedback")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("records", hasSize(1));

            verify(feedbackService).searchList("bug", "NEW", 123L, "crash", 1, 20);
            verify(feedbackService, never()).list(anyString(), anyString(), anyLong(), anyLong(), anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取反馈列表 - 空白关键词使用普通列表")
        void adminListFeedback_BlankKeyword() {
            // Given
            Page<Feedback> mockPage = createMockFeedbackPage();
            when(feedbackService.list(null, null, null, 1, 20)).thenReturn(mockPage);

            // When & Then - 测试空白关键词
            given()
                .param("keyword", "   ")
                .param("page", 1)
                .param("size", 20)
            .when()
                .get("/api/v1/admin/feedback")
            .then()
                .statusCode(HttpStatus.OK.value());

            verify(feedbackService).list(null, null, null, 1, 20);
            verify(feedbackService, never()).searchList(anyString(), anyString(), anyLong(), anyString(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("管理员获取反馈列表 - 无权限")
        void adminListFeedback_Unauthorized() {
            // When & Then - 无权限访问
            when()
                .get("/api/v1/admin/feedback")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("管理员获取反馈列表 - 权限不足")
        void adminListFeedback_InsufficientRole() {
            // When & Then - USER角色无权访问
            when()
                .get("/api/v1/admin/feedback")
            .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取反馈详情 - 成功")
        void adminGetFeedbackDetail_Success() {
            // Given
            Feedback feedback = createMockFeedback();
            when(feedbackService.detail(1L)).thenReturn(feedback);

            // When & Then
            when()
                .get("/api/v1/admin/feedback/1")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(1))
                .body("type", equalTo("bug"))
                .body("content", equalTo("App crashes when opening"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取反馈详情 - 未找到")
        void adminGetFeedbackDetail_NotFound() {
            // Given
            when(feedbackService.detail(999L)).thenReturn(null);

            // When & Then
            when()
                .get("/api/v1/admin/feedback/999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员更新反馈状态 - 成功")
        void adminUpdateFeedbackStatus_Success() {
            // Given
            UpdateFeedbackStatusRequest request = new UpdateFeedbackStatusRequest();
            request.setStatus("RESOLVED");
            request.setRemark("Fixed in version 2.1");

            when(feedbackService.updateStatus(eq(1L), eq("RESOLVED"), eq("Fixed in version 2.1"), any()))
                    .thenReturn(1);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/api/v1/admin/feedback/1/status")
            .then()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员更新反馈状态 - 未找到")
        void adminUpdateFeedbackStatus_NotFound() {
            // Given
            UpdateFeedbackStatusRequest request = new UpdateFeedbackStatusRequest();
            request.setStatus("RESOLVED");
            request.setRemark("Test remark");

            when(feedbackService.updateStatus(eq(999L), anyString(), anyString(), any())).thenReturn(0);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/api/v1/admin/feedback/999/status")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员删除反馈 - 成功")
        void adminDeleteFeedback_Success() {
            // Given
            when(feedbackService.delete(1L)).thenReturn(1);

            // When & Then
            when()
                .delete("/api/v1/admin/feedback/1")
            .then()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员删除反馈 - 未找到")
        void adminDeleteFeedback_NotFound() {
            // Given
            when(feedbackService.delete(999L)).thenReturn(0);

            // When & Then
            when()
                .delete("/api/v1/admin/feedback/999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员获取统计信息")
        void adminGetStats() {
            // Given
            Map<String, Object> stats = createMockStats();
            when(feedbackService.getStats()).thenReturn(stats);

            // When & Then
            when()
                .get("/api/v1/admin/feedback/stats")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalCount", equalTo(100))
                .body("statusStats.NEW", equalTo(20))
                .body("typeStats.bug", equalTo(30));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员批量更新状态 - 成功")
        void adminBatchUpdateStatus_Success() {
            // Given
            BatchUpdateStatusRequest request = new BatchUpdateStatusRequest();
            request.setIds(Arrays.asList(1L, 2L, 3L));
            request.setStatus("RESOLVED");
            request.setRemark("Batch resolved");

            when(feedbackService.batchUpdateStatus(eq(Arrays.asList(1L, 2L, 3L)), eq("RESOLVED"), eq("Batch resolved"), any()))
                    .thenReturn(3);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/api/v1/admin/feedback/batch/status")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("updatedCount", equalTo(3));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员批量删除 - 成功")
        void adminBatchDelete_Success() {
            // Given
            List<Long> ids = Arrays.asList(1L, 2L, 3L);
            when(feedbackService.batchDelete(ids)).thenReturn(3);

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(ids)
            .when()
                .delete("/api/v1/admin/feedback/batch")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("deletedCount", equalTo(3));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("管理员批量更新状态 - 验证失败")
        void adminBatchUpdateStatus_ValidationFailure() {
            // Given - 空的ID列表
            BatchUpdateStatusRequest request = new BatchUpdateStatusRequest();
            request.setIds(Collections.emptyList());
            request.setStatus("RESOLVED");

            // When & Then
            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .put("/api/v1/admin/feedback/batch/status")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
        }
    }

    // Helper methods
    private SubmitFeedbackRequest createValidSubmitRequest() {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setType("bug");
        request.setContent("App crashes when opening");
        request.setContact("user@example.com");
        request.setAppVersion("1.0.0");
        request.setOsVersion("iOS 15");
        request.setDeviceModel("iPhone 13");
        request.setNetworkType("wifi");
        request.setPageRoute("/home");
        request.setUserId(123L);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("custom", "data");
        request.setExtraData(extraData);

        return request;
    }

    private Feedback createMockFeedback() {
        Feedback feedback = new Feedback();
        feedback.setId(1L);
        feedback.setType("bug");
        feedback.setContent("App crashes when opening");
        feedback.setStatus("NEW");
        feedback.setUserId(123L);
        feedback.setCreatedTime(LocalDateTime.now());
        return feedback;
    }

    private Page<Feedback> createMockFeedbackPage() {
        Page<Feedback> page = new Page<>(1, 20);
        page.setRecords(Collections.singletonList(createMockFeedback()));
        page.setTotal(1);
        return page;
    }

    private Map<String, Object> createMockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", 100);

        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("NEW", 20L);
        statusStats.put("IN_PROGRESS", 30L);
        statusStats.put("RESOLVED", 40L);
        statusStats.put("REJECTED", 10L);
        stats.put("statusStats", statusStats);

        Map<String, Long> typeStats = new HashMap<>();
        typeStats.put("bug", 30L);
        typeStats.put("suggestion", 25L);
        typeStats.put("content", 20L);
        typeStats.put("ux", 15L);
        typeStats.put("other", 10L);
        stats.put("typeStats", typeStats);

        return stats;
    }
}
