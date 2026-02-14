package com.bqsummer.controller;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.auth.UserProfile;
import com.bqsummer.common.vo.req.auth.AdminCreateUserRequest;
import com.bqsummer.constant.UserType;
import com.bqsummer.framework.security.TokenBlacklistService;
import com.bqsummer.mapper.RefreshTokenMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.UserProfileMapper;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserProfileMapper userProfileMapper;

    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.findById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile/ext")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
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
            profile = UserProfile.builder().userId(userId).build();
        }
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile/ext")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> upsertCurrentUserExtProfile(@RequestBody UserProfile req,
                                                              HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }
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
        return ResponseEntity.ok("saved");
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> updateProfile(@RequestBody User updateRequest,
                                                HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return ResponseEntity.badRequest().body("failed");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.badRequest().body("failed");
        }
        return ResponseEntity.ok("updated");
    }

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
            return ResponseEntity.notFound().build();
        }
        refreshTokenMapper.deleteByUserId(userId);
        long expiresAt = jwtUtil.getExpirationMillis(token);
        if (expiresAt > 0L) {
            tokenBlacklistService.add(token, expiresAt);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;
        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        List<User> users = userMapper.findPageForAdmin(safeKeyword, offset, safePageSize);
        long total = userMapper.countForAdmin(safeKeyword);
        users.forEach(user -> user.setPassword(null));

        Map<String, Object> data = new HashMap<>();
        data.put("list", users);
        data.put("total", total);
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        data.put("totalPages", (total + safePageSize - 1) / safePageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(error("username already exists"));
        }
        if (userMapper.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(error("email already exists"));
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .phone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null)
                .nickName(StringUtils.hasText(request.getNickName()) ? request.getNickName().trim() : null)
                .password(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()))
                .status(1)
                .userType(UserType.REAL.getCode())
                .build();

        userMapper.insertWithType(user);
        Long roleId = userMapper.findRoleIdByName("ROLE_USER");
        if (roleId != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "created");
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", -1);
        result.put("message", message);
        return result;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
