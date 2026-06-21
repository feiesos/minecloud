package org.feiesos.auth.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class AdminPermissionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    Long id;

    String code;
    String name;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
