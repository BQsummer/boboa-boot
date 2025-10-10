package com.bqsummer.controller;


import com.bqsummer.common.vo.resp.auth.AuthResponse;
import com.bqsummer.common.vo.req.auth.LoginRequest;
import com.bqsummer.common.vo.req.auth.RegisterRequest;
import com.bqsummer.framework.security.TokenBlacklistService;
import com.bqsummer.service.auth.AuthService;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Login error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Refresh token error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @RequestParam(required = false) String refreshToken) {
        String accessToken = JwtUtil.extractBearerToken(request.getHeader("Authorization"));
        if (StringUtils.hasText(accessToken) && jwtUtil.validateToken(accessToken)) {
            long expiresAt = jwtUtil.getExpirationMillis(accessToken);
            if (expiresAt > 0L) {
                tokenBlacklistService.add(accessToken, expiresAt);
            }
        }
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }


}
