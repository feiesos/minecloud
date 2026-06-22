package org.feiesos.auth.scheduled;

import org.feiesos.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final AuthService authService;

    public RefreshTokenCleanupTask(AuthService authService) {
        this.authService = authService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredRefreshTokens() {
        int deleted = authService.deleteExpiredRefreshTokens();
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh tokens", deleted);
        }
    }
}
