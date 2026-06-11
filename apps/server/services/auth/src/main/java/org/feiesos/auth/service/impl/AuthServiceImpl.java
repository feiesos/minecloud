package org.feiesos.auth.service.impl;

import org.feiesos.api.auth.dto.LoginRequest;
import org.feiesos.api.auth.dto.LoginResponse;
import org.feiesos.api.auth.dto.RegisterRequest;
import org.feiesos.api.auth.dto.RegisterResponse;
import org.feiesos.auth.entity.SysUser;
import org.feiesos.auth.mapper.UserMapper;
import org.feiesos.auth.service.AuthService;
import org.feiesos.auth.service.RateLimitService;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.security.JwtClaims;
import org.feiesos.common.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private static final int REGISTER_MAX_ATTEMPTS = 3;
    private static final Duration REGISTER_WINDOW = Duration.ofMinutes(10);

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimitService rateLimitService;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           RateLimitService rateLimitService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimitService = rateLimitService;
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

        userMapper.insert(user);

        rateLimitService.recordAttempt(registerKey);

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .verificationToken(user.getVerificationToken())
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        System.out.println("-------------verifying email--------------");
        SysUser user = userMapper.findByVerificationToken(token);
        if (user.getVerifiedAt() != null && user.getVerifiedAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("邮箱已被验证");
        }
        if (user == null) {
            throw new BusinessException("验证令牌无效或已过期");
        }

        user.setEnabled(true);
        user.setVerifiedAt(java.time.OffsetDateTime.now());
        user.setVerificationToken(null);
        userMapper.updateById(user);
    }

    @Override
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

        return LoginResponse.builder()
                .token(jwtTokenProvider.createToken(claims))
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
        userMapper.updateById(user);

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .verificationToken(user.getVerificationToken())
                .build();
    }
}
