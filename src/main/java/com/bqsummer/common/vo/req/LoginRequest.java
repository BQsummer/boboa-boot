package com.bqsummer.common.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 用户登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名/邮箱不能为空")
    private String usernameOrEmail;

    @NotBlank(message = "密码不能为空")
    private String password;
}
