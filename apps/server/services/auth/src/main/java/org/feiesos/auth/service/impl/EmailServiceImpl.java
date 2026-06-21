package org.feiesos.auth.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.feiesos.auth.config.MailProperties;
import org.feiesos.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public EmailServiceImpl(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendVerificationEmail(String to, String username, String verificationToken) {
        String verifyUrl = mailProperties.getFrontendUrl() + "/verify-email?token=" + verificationToken;
        String subject = "验证你的 Minecloud 邮箱地址";

        String html = """
                <div style="max-width:520px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif">
                  <h2 style="color:#1a1a2e">Minecloud</h2>
                  <p>你好 <strong>%s</strong>，</p>
                  <p>感谢注册 Minecloud。请点击下方按钮验证你的邮箱地址：</p>
                  <a href="%s" style="display:inline-block;padding:12px 28px;background:#2563eb;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;margin:12px 0">
                    验证邮箱
                  </a>
                  <p style="margin-top:16px;color:#6b7280;font-size:0.88em">
                    如果你没有注册 Minecloud 账号，请忽略此邮件。<br>
                    此链接将在 24 小时后过期。
                  </p>
                </div>
                """.formatted(username, verifyUrl);

        send(to, subject, html, verificationToken);
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        String resetUrl = mailProperties.getFrontendUrl() + "/reset-password?token=" + resetToken;
        String subject = "重置你的 Minecloud 密码";

        String html = """
                <div style="max-width:520px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif">
                  <h2 style="color:#1a1a2e">Minecloud</h2>
                  <p>你好 <strong>%s</strong>，</p>
                  <p>我们收到了你重置密码的请求。请点击下方按钮设置新密码：</p>
                  <a href="%s" style="display:inline-block;padding:12px 28px;background:#2563eb;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;margin:12px 0">
                    重置密码
                  </a>
                  <p style="margin-top:16px;color:#6b7280;font-size:0.88em">
                    如果你没有请求重置密码，请忽略此邮件。<br>
                    此链接将在 1 小时后过期。
                  </p>
                </div>
                """.formatted(username, resetUrl);

        send(to, subject, html, resetToken);
    }

    private void send(String to, String subject, String html, String token) {
        boolean mailConfigured = mailSender != null;
        if (!mailConfigured) {
            log.warn("邮件服务未配置，跳过发送。验证令牌: {}", token);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getMailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("邮件已发送至: {}", to);
        } catch (MailException | MessagingException e) {
            log.error("邮件发送失败 ({}): {}", to, e.getMessage());
        }
    }
}
