package com.bqsummer.controller;

import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器 - 演示管理员权限的接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;

    /**
     * 获取系统统计信息 - 需要ADMIN角色
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100); // 这里应该从数据库查询实际数据
        stats.put("activeUsers", 85);
        stats.put("todayRegistrations", 5);
        stats.put("systemStatus", "运行正常");

        return ResponseEntity.ok(stats);
    }

    /**
     * 禁用/启用用户 - 需要ADMIN角色
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserStatus(@PathVariable Long userId,
                                                 @RequestParam Integer status) {
        // 这里应该添加更新用户状态的逻辑
        return ResponseEntity.ok(String.format("用户ID %d 状态已更新为 %s",
                                             userId, status == 1 ? "启用" : "禁用"));
    }

    /**
     * 系统配置管理 - 需要ADMIN角色
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("jwtExpiration", "24小时");
        config.put("maxLoginAttempts", 5);
        config.put("passwordPolicy", "最少6位，包含字母和数字");

        return ResponseEntity.ok(config);
    }
}
