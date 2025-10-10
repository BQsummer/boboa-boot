package com.bqsummer.controller;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.auth.UserProfile;
import com.bqsummer.framework.security.TokenBlacklistService;
import com.bqsummer.mapper.RefreshTokenMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.UserProfileMapper;
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
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserProfileMapper userProfileMapper;

    /**
     * 获取当前用户信息 - 需要USER角色
     */
    @GetMapping("/profile")
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
     * 获取当前用户的扩展资料
     */
    @GetMapping("/profile/ext")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfile> getCurrentUserExtProfile(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            // 返回一个仅包含userId的空资料，便于前端渲染和后续提交
            profile = UserProfile.builder().userId(userId).build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * 更新当前用户的扩展资料（无则创建）
     */
    @PutMapping("/profile/ext")
    public ResponseEntity<String> upsertCurrentUserExtProfile(@RequestBody UserProfile req,
                                                              HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("未授权");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body("未授权");
        }
        // 服务端强制绑定当前用户，忽略客户端传入的userId和id
        UserProfile toSave = UserProfile.builder()
                .userId(userId)
                .gender(req.getGender())
                .birthday(req.getBirthday())
                .heightCm(req.getHeightCm())
                .mbti(req.getMbti())
                .occupation(req.getOccupation())
                .interests(req.getInterests())
                .photos(req.getPhotos())
                .build();
        userProfileMapper.upsert(toSave);
        return ResponseEntity.ok("扩展资料已保存");
    }

    /**
     * 更新用户信息 - 需要USER角色
     */
    @PutMapping("/profile")
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
     * 软删除当前账号（自删）- 需要USER角色
     */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteCurrentUser(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        int updated = userMapper.softDelete(userId);
        if (updated == 0) {
            // 已删除或不存在
            return ResponseEntity.notFound().build();
        }
        // 清理该用户的刷新令牌
        refreshTokenMapper.deleteByUserId(userId);
        // 当前访问令牌加入黑名单
        long expiresAt = jwtUtil.getExpirationMillis(token);
        if (expiresAt > 0L) {
            tokenBlacklistService.add(token, expiresAt);
        }
        return ResponseEntity.ok().build();
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
