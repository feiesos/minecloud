package org.feiesos.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minecloud")
public class MailProperties {
    private String frontendUrl = "http://localhost:5173";
    private String mailFrom = "noreply@minecloud";
}
