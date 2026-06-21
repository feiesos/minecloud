package org.feiesos.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.feiesos.api.auth.dto.ForgotPasswordRequest;
import org.feiesos.api.auth.dto.LoginRequest;
import org.feiesos.api.auth.dto.LoginResponse;
import org.feiesos.api.auth.dto.RefreshRequest;
import org.feiesos.api.auth.dto.RegisterRequest;
import org.feiesos.api.auth.dto.RegisterResponse;
import org.feiesos.api.auth.dto.ResetPasswordRequest;
import org.feiesos.common.result.R;
import org.feiesos.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public R<RegisterResponse> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        RegisterResponse response = authService.register(request, clientIp);
        return R.ok(response);
    }

    @PostMapping("/verify-email")
    public R<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return R.ok();
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.ok(response);
    }

    @PostMapping("/resend-verification")
    public R<RegisterResponse> resendVerification(@RequestParam String email) {
        RegisterResponse response = authService.resendVerification(email);
        return R.ok(response);
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return R.ok(response);
    }

    @PostMapping("/forgot-password")
    public R<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                   HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        authService.forgotPassword(request.getEmail(), clientIp);
        return R.ok();
    }

    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return R.ok();
    }

    @GetMapping("/permission/check")
    public R<Boolean> checkPermission(@RequestParam Long userId,
                                       @RequestParam String permissionCode) {
        boolean hasPermission = authService.checkPermission(userId, permissionCode);
        return R.ok(hasPermission);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
