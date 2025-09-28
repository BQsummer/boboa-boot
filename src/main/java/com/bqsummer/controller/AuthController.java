package com.bqsummer.controller;


import com.bqsummer.common.vo.resp.AuthResponse;
import com.bqsummer.common.vo.req.LoginRequest;
import com.bqsummer.common.vo.req.RegisterRequest;
import com.bqsummer.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
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
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam(required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }
}
