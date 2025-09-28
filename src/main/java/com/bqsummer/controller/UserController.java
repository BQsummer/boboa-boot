package com.bqsummer.controller;

import com.bqsummer.common.dto.User;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 获取当前用户信息 - 需要USER角色
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<User> getCurrentUserProfile(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            User user = userMapper.findById(userId);
            if (user != null) {
                // 不返回密码
                user.setPassword(null);
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 更新用户信息 - 需要USER角色
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> updateProfile(@RequestBody User updateRequest,
                                              HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 这里应该添加更新用户信息的逻辑
            // 为了演示，只返回成功消息
            return ResponseEntity.ok("用户信息更新成功");
        }
        return ResponseEntity.badRequest().body("更新失败");
    }

    /**
     * 获取用户列表 - 需要ADMIN角色
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getUserList() {
        // 这里应该返回用户列表
        return ResponseEntity.ok("用户列表数据 - 仅管理员可访问");
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
