package com.bqsummer.controller;

import com.bqsummer.common.dto.invite.InviteCode;
import com.bqsummer.common.vo.req.invite.CreateInviteCodeRequest;
import com.bqsummer.common.vo.req.invite.RedeemInviteRequest;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.service.InviteService;
import com.bqsummer.util.JwtUtil;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * InviteController RestAssured MockMvc tests covering success and error branches.
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class InviteControllerTest {

    @org.mockito.Mock
    InviteService inviteService;
    @org.mockito.Mock
    JwtUtil jwtUtil;

    InviteController controller;

    @BeforeEach
    void setup() {
        controller = new InviteController(inviteService, jwtUtil);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionAdvice())
                .build();
        mockMvc(mockMvc);
        // Note: JwtUtil.getUserIdFromToken is static in code; we don't rely on it.
    }

    // ========================= /api/v1/invite/codes =========================

    /**
     * 测试目的：创建邀请码-入参合法时返回200并包含关键字段
     */
    @Test
    @DisplayName("shouldCreateInviteCodeWhenRequestIsValid")
    void shouldCreateInviteCodeWhenRequestIsValid() {
        // Arrange: mock service success, ignore userId
        InviteCode ic = new InviteCode();
        ic.setId(123L);
        ic.setCode("ABCDEF1234");
        ic.setMaxUses(3);
        ic.setUsedCount(0);
        ic.setStatus("ACTIVE");
        ic.setExpireAt(LocalDateTime.now().plusDays(7));
        org.mockito.BDDMockito.given(inviteService.createCode(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(CreateInviteCodeRequest.class))).willReturn(ic);

        // Act & Assert
        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"maxUses\":3,\"remark\":\"for friends\"}")
        .when()
                .post("/api/v1/invite/codes")
        .then()
                .statusCode(200)
                .body("id", equalTo(123))
                .body("code", equalTo("ABCDEF1234"))
                .body("maxUses", equalTo(3))
                .body("usedCount", equalTo(0))
                .body("status", equalTo("ACTIVE"))
                .body("expireAt", notNullValue());
    }

    /**
     * 测试目的：创建邀请码-maxUses=0 触发校验错误，返回400
     */
    @Test
    @DisplayName("shouldReturn400WhenCreateInviteCodeWithInvalidMaxUses")
    void shouldReturn400WhenCreateInviteCodeWithInvalidMaxUses() {
        // Purpose: @Valid should catch maxUses=0 and return 400
        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"maxUses\":0}")
        .when()
                .post("/api/v1/invite/codes")
        .then()
                .statusCode(400);
    }

    /**
     * 测试目的：创建邀请码-未携带Authorization，返回401
     */
    @Test
    @DisplayName("shouldReturn401WhenCreateInviteCodeWithoutAuth")
    void shouldReturn401WhenCreateInviteCodeWithoutAuth() {
        // Purpose: no Authorization header -> service throws auth error
        org.mockito.BDDMockito.given(inviteService.createCode(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(CreateInviteCodeRequest.class)))
                .willThrow(new BadCredentialsException("no token"));

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"maxUses\":2}")
        .when()
                .post("/api/v1/invite/codes")
        .then()
                .statusCode(401)
                .body("code", equalTo(401))
                .body("message", containsString("no token"));
    }

    /**
     * 测试目的：创建邀请码-服务端异常，返回500
     */
    @Test
    @DisplayName("shouldReturn500WhenCreateInviteCodeServerError")
    void shouldReturn500WhenCreateInviteCodeServerError() {
        org.mockito.BDDMockito.given(inviteService.createCode(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(CreateInviteCodeRequest.class)))
                .willThrow(new RuntimeException("db down"));

        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"maxUses\":2}")
        .when()
                .post("/api/v1/invite/codes")
        .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("message", containsString("db down"));
    }

    // ========================= /api/v1/invite/codes/my =========================

    /**
     * 测试目的：查询我的邀请码-授权成功返回列表200
     */
    @Test
    @DisplayName("shouldListMyCodesWhenAuthorized")
    void shouldListMyCodesWhenAuthorized() {
        InviteCode a = new InviteCode();
        a.setId(1L); a.setCode("C1"); a.setMaxUses(5); a.setUsedCount(1); a.setStatus("ACTIVE"); a.setExpireAt(LocalDateTime.now().plusDays(1));
        InviteCode b = new InviteCode();
        b.setId(2L); b.setCode("C2"); b.setMaxUses(2); b.setUsedCount(2); b.setStatus("USED"); b.setExpireAt(LocalDateTime.now().plusDays(2));
        org.mockito.BDDMockito.given(inviteService.listMyCodes(org.mockito.ArgumentMatchers.any())).willReturn(List.of(a, b));

        MockMvcResponse resp =
        given()
                .header("Authorization", "Bearer token")
        .when()
                .get("/api/v1/invite/codes/my")
        .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].code", equalTo("C1"))
                .body("[1].status", equalTo("USED"))
                .extract()
                .response();

        // Additional grouped assertions
        List<Map<String, Object>> list = resp.as(List.class);
        assertAll(
                () -> assertEquals(2, list.size()),
                () -> assertEquals("C2", list.get(1).get("code"))
        );
    }

    /**
     * 测试目的：查询我的邀请码-未授权401
     */
    @Test
    @DisplayName("shouldReturn401WhenListMyCodesWithoutAuth")
    void shouldReturn401WhenListMyCodesWithoutAuth() {
        org.mockito.BDDMockito.given(inviteService.listMyCodes(org.mockito.ArgumentMatchers.isNull())).willThrow(new BadCredentialsException("no auth"));

        given()
        .when()
                .get("/api/v1/invite/codes/my")
        .then()
                .statusCode(401)
                .body("code", equalTo(401))
                .body("message", containsString("no auth"));
    }

    // ========================= /api/v1/invite/validate =========================

    /**
     * 测试目的：校验邀请码-有效时返回valid=true
     */
    @Test
    @DisplayName("shouldValidateInviteCodeWhenCodeIsActive")
    void shouldValidateInviteCodeWhenCodeIsActive() {
        var vr = InviteService.ValidateResult.valid(3, LocalDateTime.now().plusDays(1));
        org.mockito.BDDMockito.given(inviteService.validateCode(org.mockito.ArgumentMatchers.eq("ABCD"))).willReturn(vr);

        given()
                .queryParam("code", "ABCD")
        .when()
                .post("/api/v1/invite/validate")
        .then()
                .statusCode(200)
                .body("valid", equalTo(true))
                .body("status", equalTo("ACTIVE"))
                .body("remainingUses", equalTo(3))
                .body("expireAt", notNullValue());
    }

    /**
     * 测试目的：校验邀请码-不存在时valid=false且状态为NOT_FOUND
     */
    @Test
    @DisplayName("shouldReturn200WithInvalidStatusWhenCodeNotFound")
    void shouldReturn200WithInvalidStatusWhenCodeNotFound() {
        var vr = InviteService.ValidateResult.invalid("NOT_FOUND");
        org.mockito.BDDMockito.given(inviteService.validateCode(org.mockito.ArgumentMatchers.eq("XYZ"))).willReturn(vr);

        given()
                .queryParam("code", "XYZ")
        .when()
                .post("/api/v1/invite/validate")
        .then()
                .statusCode(200)
                .body("valid", equalTo(false))
                .body("status", equalTo("NOT_FOUND"))
                .body("remainingUses", equalTo(0));
    }

    /**
     * 测试目的：校验邀请码-缺少必填参数code返回400
     */
    @Test
    @DisplayName("shouldReturn400WhenValidateWithoutCodeParam")
    void shouldReturn400WhenValidateWithoutCodeParam() {
        when()
                .post("/api/v1/invite/validate")
        .then()
                .statusCode(400);
    }

    // ========================= /api/v1/invite/redeem =========================

    /**
     * 测试目的：兑换邀请码-入参合法时返回200并包含剩余次数
     */
    @Test
    @DisplayName("shouldRedeemInviteCodeWhenRequestIsValid")
    void shouldRedeemInviteCodeWhenRequestIsValid() {
        org.mockito.BDDMockito.given(inviteService.redeem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(RedeemInviteRequest.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .willReturn(new InviteService.RedeemResult(true, 2));

        given()
                .header("Authorization", "Bearer token")
                .header("X-Forwarded-For", "1.2.3.4")
                .header("User-Agent", "JUnit")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"HELLO123\"}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("remainingUses", equalTo(2));
    }

    /**
     * 测试目的：兑换邀请码-缺失必填字段code返回400
     */
    @Test
    @DisplayName("shouldReturn400WhenRedeemWithMissingCode")
    void shouldReturn400WhenRedeemWithMissingCode() {
        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(400);
    }

    /**
     * 测试目的：兑换邀请码-code不存在返回404
     */
    @Test
    @DisplayName("shouldReturn404WhenRedeemWithInvalidCode")
    void shouldReturn404WhenRedeemWithInvalidCode() {
        org.mockito.BDDMockito.given(inviteService.redeem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(RedeemInviteRequest.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .willThrow(new SnorlaxClientException(404, "invalid code"));

        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"NOPE\"}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("message", containsString("invalid code"));
    }

    /**
     * 测试目的：兑换邀请码-并发冲突返回409
     */
    @Test
    @DisplayName("shouldReturn409WhenRedeemConflict")
    void shouldReturn409WhenRedeemConflict() {
        org.mockito.BDDMockito.given(inviteService.redeem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(RedeemInviteRequest.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .willThrow(new SnorlaxClientException(409, "code cannot be consumed now"));

        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"BUSY\"}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(409)
                .body("code", equalTo(409))
                .body("message", containsString("cannot be consumed"));
    }

    /**
     * 测试目的：兑换邀请码-IP风控返回429
     */
    @Test
    @DisplayName("shouldReturn429WhenRedeemRateLimited")
    void shouldReturn429WhenRedeemRateLimited() {
        org.mockito.BDDMockito.given(inviteService.redeem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(RedeemInviteRequest.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .willThrow(new SnorlaxClientException(429, "too many redeems from this IP"));

        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"LIMIT\"}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(429)
                .body("code", equalTo(429))
                .body("message", containsString("too many"));
    }

    /**
     * 测试目的：兑换邀请码-服务端异常返回500
     */
    @Test
    @DisplayName("shouldReturn500WhenRedeemServerError")
    void shouldReturn500WhenRedeemServerError() {
        org.mockito.BDDMockito.given(inviteService.redeem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(RedeemInviteRequest.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .willThrow(new RuntimeException("unexpected"));

        given()
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"ERR\"}")
        .when()
                .post("/api/v1/invite/redeem")
        .then()
                .statusCode(500)
                .body("code", equalTo(500))
                .body("message", containsString("unexpected"));
    }

    // ========================= /api/v1/invite/codes/{id}/revoke =========================

    /**
     * 测试目的：撤销邀请码-授权且有权限时返回200
     */
    @Test
    @DisplayName("shouldRevokeCodeWhenAuthorizedAndOwner")
    void shouldRevokeCodeWhenAuthorizedAndOwner() {
        doNothing().when(inviteService).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(10L));

        given()
                .header("Authorization", "Bearer token")
                .pathParam("id", 10)
        .when()
                .post("/api/v1/invite/codes/{id}/revoke")
        .then()
                .statusCode(200);
    }

    /**
     * 测试目的：撤销邀请码-权限不足返回403
     */
    @Test
    @DisplayName("shouldReturn403WhenRevokeWithoutPermission")
    void shouldReturn403WhenRevokeWithoutPermission() {
        doThrow(new AccessDeniedException("no permission")).when(inviteService).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(11L));

        given()
                .header("Authorization", "Bearer token")
                .pathParam("id", 11)
        .when()
                .post("/api/v1/invite/codes/{id}/revoke")
        .then()
                .statusCode(403)
                .body("code", equalTo(403))
                .body("message", containsString("no permission"));
    }

    /**
     * 测试目的：撤销邀请码-资源不存在返回404
     */
    @Test
    @DisplayName("shouldReturn404WhenRevokeNonExistingCode")
    void shouldReturn404WhenRevokeNonExistingCode() {
        doThrow(new SnorlaxClientException(404, "invite code not found")).when(inviteService).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(404L));

        given()
                .header("Authorization", "Bearer token")
                .pathParam("id", 404)
        .when()
                .post("/api/v1/invite/codes/{id}/revoke")
        .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("message", containsString("not found"));
    }

    /**
     * 测试目的：撤销邀请码-非法路径参数返回400
     */
    @Test
    @DisplayName("shouldReturn400WhenRevokeWithIllegalId")
    void shouldReturn400WhenRevokeWithIllegalId() {
        doThrow(new SnorlaxClientException(400, "invalid id")).when(inviteService).revoke(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(-1L));

        given()
                .header("Authorization", "Bearer token")
                .pathParam("id", -1)
        .when()
                .post("/api/v1/invite/codes/{id}/revoke")
        .then()
                .statusCode(400)
                .body("code", equalTo(400))
                .body("message", containsString("invalid id"));
    }

    // ========================= /api/v1/invite/my/stats =========================

    /**
     * 测试目的：我的邀请统计-授权成功返回统计信息
     */
    @Test
    @DisplayName("shouldReturnMyStatsWhenAuthorized")
    void shouldReturnMyStatsWhenAuthorized() {
        InviteCode a = new InviteCode(); a.setMaxUses(5); a.setUsedCount(2);
        InviteCode b = new InviteCode(); b.setMaxUses(3); b.setUsedCount(1);
        org.mockito.BDDMockito.given(inviteService.listMyCodes(org.mockito.ArgumentMatchers.any())).willReturn(List.of(a, b));

        given()
                .header("Authorization", "Bearer token")
        .when()
                .get("/api/v1/invite/my/stats")
        .then()
                .statusCode(200)
                .body("totalCodes", equalTo(2))
                .body("totalUses", equalTo(3))
                .body("totalRemaining", equalTo(5));
    }

    /**
     * 测试目的：我的邀请统计-未授权返回401
     */
    @Test
    @DisplayName("shouldReturn401WhenGetStatsWithoutAuth")
    void shouldReturn401WhenGetStatsWithoutAuth() {
        org.mockito.BDDMockito.given(inviteService.listMyCodes(org.mockito.ArgumentMatchers.isNull())).willThrow(new BadCredentialsException("auth required"));

        when()
                .get("/api/v1/invite/my/stats")
        .then()
                .statusCode(401)
                .body("code", equalTo(401))
                .body("message", containsString("auth required"));
    }

    // ========================= 404 unknown path =========================

    /**
     * 测试目的：访问不存在的路径返回404
     */
    @Test
    @DisplayName("shouldReturn404WhenPathNotFound")
    void shouldReturn404WhenPathNotFound() {
        when()
                .get("/api/v1/invite/unknown/path")
        .then()
                .statusCode(404);
    }

    // -------------------------- Test-only global exception handler --------------------------
    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, Object>> handle401(Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("code", 401, "message", e.getMessage()));
        }
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handle403(Exception e) {
            return ResponseEntity.status(403)
                    .body(Map.of("code", 403, "message", e.getMessage()));
        }
        @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentNotValidException.class})
        public ResponseEntity<Map<String, Object>> handle400(Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of("code", 400, "message", String.valueOf(e.getMessage())));
        }
        @ExceptionHandler(SnorlaxClientException.class)
        public ResponseEntity<Map<String, Object>> handleClient(SnorlaxClientException e) {
            int status = e.getCode() > 0 ? e.getCode() : 400;
            return ResponseEntity.status(status)
                    .body(Map.of("code", status, "message", e.getMessage()));
        }
        @ExceptionHandler(Throwable.class)
        public ResponseEntity<Map<String, Object>> handle500(Throwable e) {
            return ResponseEntity.status(500)
                    .body(Map.of("code", 500, "message", String.valueOf(e.getMessage())));
        }
    }
}

