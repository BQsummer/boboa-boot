package com.bqsummer.controller;

import com.bqsummer.configuration.Configs;
import com.bqsummer.framework.http.HttpClientTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器 - 演示不同权限级别的接口
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    @Autowired
    private Configs configs;

    @Autowired
    private HttpClientTemplate template;


    @GetMapping("test2")
    public String home2() {
        template.doGet("http://baidu.com");
        return configs.getTestValue();
    }

    /**
     * 公开接口 - 无需认证
     */
    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("这是公开接口，无需认证即可访问");
    }

    /**
     * 需要认证的接口 - 任何登录用户都可访问
     */
    @GetMapping("/authenticated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> authenticatedEndpoint() {
        return ResponseEntity.ok("这是认证接口，需要登录后访问");
    }

    /**
     * 需要USER角色的接口
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> userEndpoint() {
        return ResponseEntity.ok("这是普通用户接口，需要USER角色");
    }

    /**
     * 需要ADMIN角色的接口
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("这是管理员接口，需要ADMIN角色");
    }
}
