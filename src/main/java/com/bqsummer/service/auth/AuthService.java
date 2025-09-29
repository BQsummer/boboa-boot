package com.bqsummer.service.auth;


import com.bqsummer.common.dto.*;
import com.bqsummer.common.vo.req.LoginRequest;
import com.bqsummer.common.vo.req.RegisterRequest;
import com.bqsummer.common.vo.resp.AuthResponse;
import com.bqsummer.framework.security.TokenBlacklistService;
import com.bqsummer.mapper.RefreshTokenMapper;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()))
                .nickName(request.getNickName() != null ? request.getNickName() : request.getUsername())
                .status(1)
                .build();

        userMapper.insert(user);

        // 分配默认角色
        Long roleId = userMapper.findRoleIdByName("ROLE_USER");
        if (roleId != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }

        // 生成令牌
        List<String> roles = List.of("ROLE_USER");
        return generateAuthResponse(user, roles);
    }

    /**
     * 用户登录
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userMapper.findByUsernameOrEmail(request.getUsernameOrEmail());
        if (user == null) {
            log.warn("登录失败，用户不存在: {}", request.getUsernameOrEmail());
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查账户状态
        if (user.getStatus() == 0) {
            log.warn("登录失败，账户已被禁用: {}", request.getUsernameOrEmail());
            throw new RuntimeException("账户已被禁用");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            log.warn("登录失败，密码错误: {}", request.getUsernameOrEmail());
            throw new RuntimeException("用户名或密码错误");
        }

        // 更新最后登录时间
        userMapper.updateLastLoginTime(user.getId(), LocalDateTime.now());

        // 获取用户角色
        List<String> roles = user.getRoles() != null ?
                user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList()) :
                List.of("ROLE_USER");

        // 清除旧的刷新令牌
        refreshTokenMapper.deleteByUserId(user.getId());

        return generateAuthResponse(user, roles);
    }

    /**
     * 刷新令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenMapper.findByToken(refreshToken);
        if (token == null) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 获取用户信息
        User user = userMapper.findById(token.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        // 获取用户角色
        List<Role> userRoles = userMapper.findRolesByUserId(user.getId());
        List<String> roles = userRoles.stream().map(Role::getRoleName).collect(Collectors.toList());

        // 删除旧的刷新令牌
        refreshTokenMapper.deleteByToken(refreshToken);

        return generateAuthResponse(user, roles);
    }

    /**
     * 登出：将当前访问令牌加入黑名单，并删除可选的刷新令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(String accessToken, String refreshToken) {
        // 处理访问令牌：加入黑名单，过期时间与JWT自身过期时间一致
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            long expiresAt = jwtUtil.getExpirationMillis(accessToken);
            if (expiresAt > 0L) {
                tokenBlacklistService.add(accessToken, expiresAt);
            }
        }
        // 处理刷新令牌：从数据库中删除
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenMapper.deleteByToken(refreshToken);
        }
    }

    /**
     * 兼容旧签名：仅删除刷新令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(String refreshToken) {
        logout(null, refreshToken);
    }

    /**
     * 生成认证响应
     */
    private AuthResponse generateAuthResponse(User user, List<String> roles) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 保存刷新令牌
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenMapper.insert(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .roles(roles)
                .expiresIn(jwtUtil.getJwtExpiration() / 1000)
                .build();
    }
}
