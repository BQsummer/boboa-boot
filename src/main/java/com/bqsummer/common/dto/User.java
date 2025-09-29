package com.bqsummer.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String nickName;
    private String avatar;
    private Integer status;
    private Integer isDeleted; // 0: 未删除, 1: 已删除
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // 用户角色列表
    private List<Role> roles;
}
