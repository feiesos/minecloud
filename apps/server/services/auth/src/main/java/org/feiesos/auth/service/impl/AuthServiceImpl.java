package org.feiesos.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.api.auth.dto.LoginRequest;
import org.feiesos.api.auth.dto.LoginResponse;
import org.feiesos.api.auth.dto.RegisterRequest;
import org.feiesos.api.auth.dto.RegisterResponse;
import org.feiesos.auth.entity.SysRefreshToken;
import org.feiesos.auth.entity.SysRole;
import org.feiesos.auth.entity.SysUser;
import org.feiesos.auth.entity.UserRole;
import org.feiesos.auth.mapper.PermissionMapper;
import org.feiesos.auth.mapper.RefreshTokenMapper;
import org.feiesos.auth.mapper.SysRoleMapper;
import org.feiesos.auth.mapper.UserMapper;
import org.feiesos.auth.mapper.UserRoleMapper;
import org.feiesos.auth.service.AuthService;
import org.feiesos.auth.service.EmailService;
import org.feiesos.auth.service.RateLimitService;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.security.JwtClaims;
import org.feiesos.common.security.JwtProperties;
import org.feiesos.common.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private static final int REGISTER_MAX_ATTEMPTS = 3;
    private static final Duration REGISTER_WINDOW = Duration.ofMinutes(10);

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 3;
    private static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofMinutes(15);

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PermissionMapper permissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RateLimitService rateLimitService;
    private final EmailService emailService;

    public AuthServiceImpl(UserMapper userMapper,
                           RefreshTokenMapper refreshTokenMapper,
                           PermissionMapper permissionMapper,
                           SysRoleMapper sysRoleMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties,
                           RateLimitService rateLimitService,
                           EmailService emailService) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.permissionMapper = permissionMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.rateLimitService = rateLimitService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp) {
        String registerKey = "register:" + clientIp;
        if (rateLimitService.isRateLimited(registerKey, REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW)) {
            throw new BusinessException(429, "注册频率过高，请稍后再试");
        }

        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BusinessException("密码至少8位，需包含大小写字母和数字");
        }

        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }

        if (request.getEmail() != null && userMapper.findByEmail(request.getEmail()) != null) {
            throw new BusinessException("邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpireAt(OffsetDateTime.now().plusHours(24));
        userMapper.insert(user);

        assignDefaultRole(user.getId());
        rateLimitService.recordAttempt(registerKey);

        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), user.getVerificationToken());

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        SysUser user = userMapper.findByVerificationToken(token);
        if (user == null) {
            throw new BusinessException("验证令牌无效或已过期");
        }

        if (user.getVerificationTokenExpireAt() != null
                && user.getVerificationTokenExpireAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("验证令牌已过期");
        }

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("邮箱已被验证");
        }

        user.setEnabled(true);
        user.setVerifiedAt(OffsetDateTime.now());
        user.setVerificationToken(null);
        user.setVerificationTokenExpireAt(null);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String loginKey = "login:" + request.getUsername();
        if (rateLimitService.isRateLimited(loginKey, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW)) {
            throw new BusinessException(429, "登录失败次数过多，请15分钟后再试");
        }

        SysUser user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            rateLimitService.recordAttempt(loginKey);
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账户未激活，请先验证邮箱");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimitService.recordAttempt(loginKey);
            throw new BusinessException(401, "用户名或密码错误");
        }

        rateLimitService.clearAttempts(loginKey);

        JwtClaims claims = JwtClaims.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        String refreshToken = UUID.randomUUID().toString();
        SysRefreshToken refreshTokenEntity = new SysRefreshToken();
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshExpire()));
        refreshTokenEntity.setRevoked(false);
        refreshTokenMapper.insert(refreshTokenEntity);

        return LoginResponse.builder()
                .token(jwtTokenProvider.createToken(claims))
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional
    public RegisterResponse resendVerification(String email) {
        SysUser user = userMapper.findByEmail(email);
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("该邮箱已验证，无需重复验证");
        }

        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpireAt(OffsetDateTime.now().plusHours(24));
        userMapper.updateById(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), user.getVerificationToken());

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        SysRefreshToken stored = refreshTokenMapper.findByToken(refreshToken);
        if (stored == null) {
            throw new BusinessException(401, "refresh token 无效");
        }

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(401, "refresh token 已过期");
        }

        stored.setRevoked(true);
        refreshTokenMapper.updateById(stored);

        SysUser user = userMapper.selectById(stored.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(401, "用户不存在或已禁用");
        }

        JwtClaims claims = JwtClaims.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        String newRefreshToken = UUID.randomUUID().toString();
        SysRefreshToken newEntity = new SysRefreshToken();
        newEntity.setUserId(user.getId());
        newEntity.setToken(newRefreshToken);
        newEntity.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshExpire()));
        newEntity.setRevoked(false);
        refreshTokenMapper.insert(newEntity);

        return LoginResponse.builder()
                .token(jwtTokenProvider.createToken(claims))
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(String email, String clientIp) {
        String fpKey = "forgot-password:" + clientIp;
        if (rateLimitService.isRateLimited(fpKey, FORGOT_PASSWORD_MAX_ATTEMPTS, FORGOT_PASSWORD_WINDOW)) {
            throw new BusinessException(429, "操作频率过高，请稍后再试");
        }

        SysUser user = userMapper.findByEmail(email);
        if (user == null) {
            rateLimitService.recordAttempt(fpKey);
            return;
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            rateLimitService.recordAttempt(fpKey);
            return;
        }

        user.setResetPasswordToken(UUID.randomUUID().toString());
        user.setResetPasswordTokenExpireAt(OffsetDateTime.now().plusHours(1));
        userMapper.updateById(user);
        rateLimitService.recordAttempt(fpKey);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), user.getResetPasswordToken());
    }

    @Override
    public boolean validateResetToken(String token) {
        log.info("验证重置令牌: {}", token);
        SysUser user = userMapper.findByResetPasswordToken(token);
        if (user == null) {
            log.warn("未找到用户，令牌无效");
            return false;
        }
        log.info("找到用户: {}, 令牌过期时间: {}", user.getUsername(), user.getResetPasswordTokenExpireAt());
        if (user.getResetPasswordTokenExpireAt() != null
                && user.getResetPasswordTokenExpireAt().isBefore(OffsetDateTime.now())) {
            log.warn("令牌已过期");
            return false;
        }
        log.info("令牌有效");
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        SysUser user = userMapper.findByResetPasswordToken(token);
        if (user == null) {
            throw new BusinessException("重置令牌无效或已过期");
        }

        if (user.getResetPasswordTokenExpireAt() != null
                && user.getResetPasswordTokenExpireAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("重置令牌已过期");
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException("密码至少8位，需包含大小写字母和数字");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpireAt(null);
        userMapper.updateById(user);

        refreshTokenMapper.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        SysRefreshToken stored = refreshTokenMapper.findByToken(refreshToken);
        if (stored != null) {
            stored.setRevoked(true);
            refreshTokenMapper.updateById(stored);
        }
        // Silently succeed if token is already gone — idempotent
    }

    @Override
    public int deleteExpiredRefreshTokens() {
        return refreshTokenMapper.deleteExpired();
    }

    @Override
    public boolean checkPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }
        return permissionMapper.countPermission(userId, permissionCode) > 0;
    }

    private void assignDefaultRole(Long userId) {
        SysRole defaultRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "user"));
        if (defaultRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }
    }
}
