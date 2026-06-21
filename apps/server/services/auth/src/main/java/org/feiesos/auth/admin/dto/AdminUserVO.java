package org.feiesos.auth.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class AdminUserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    Long id;

    String username;
    String nickname;
    String email;
    String avatar;
    Boolean enabled;
    OffsetDateTime verifiedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    List<AdminRoleBriefVO> roles;

    @Value
    @Builder
    public static class AdminRoleBriefVO {
        @JsonSerialize(using = ToStringSerializer.class)
        Long id;
        String code;
        String name;
    }
}
