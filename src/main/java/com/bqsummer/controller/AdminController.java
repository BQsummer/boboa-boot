package com.bqsummer.controller;

import com.alibaba.druid.stat.DruidStatManagerFacade;
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
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/druid/stat")
    public Object druidStat() {
        return DruidStatManagerFacade.getInstance().getDataSourceStatDataList();
    }

}
