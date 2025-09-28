package com.bqsummer.common.vo.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 认证响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String nickName;
    private List<String> roles;
    private Long expiresIn; // 访问令牌过期时间（秒）
}
