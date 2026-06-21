package org.feiesos.auth.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class AdminRoleVO {

    @JsonSerialize(using = ToStringSerializer.class)
    Long id;

    String code;
    String name;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    List<AdminPermissionBriefVO> permissions;

    @Value
    @Builder
    public static class AdminPermissionBriefVO {
        @JsonSerialize(using = ToStringSerializer.class)
        Long id;
        String code;
        String name;
    }
}
