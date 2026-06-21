package org.feiesos.auth.service;

public interface EmailService {

    void sendVerificationEmail(String to, String username, String verificationToken);

    void sendPasswordResetEmail(String to, String username, String resetToken);
}
