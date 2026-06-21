package org.feiesos.auth.service;

import org.feiesos.api.auth.dto.LoginRequest;
import org.feiesos.api.auth.dto.LoginResponse;
import org.feiesos.api.auth.dto.RegisterRequest;
import org.feiesos.api.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request, String clientIp);

    void verifyEmail(String token);

    LoginResponse login(LoginRequest request);

    RegisterResponse resendVerification(String email);

    LoginResponse refreshToken(String refreshToken);

    boolean checkPermission(Long userId, String permissionCode);

    void forgotPassword(String email, String clientIp);

    void resetPassword(String token, String newPassword);
}
