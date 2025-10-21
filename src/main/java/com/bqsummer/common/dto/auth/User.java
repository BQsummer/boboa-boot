package com.bqsummer.common.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String username;
    private String nickName;
    private String password;
    private String email;
    private String avatar;
    private String phone;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    private Integer isDeleted; // 0: 未删除, 1: 已删除
    private LocalDateTime lastLoginTime;

    // 用户角色列表
    private List<Role> roles;

    private String userType;
}
