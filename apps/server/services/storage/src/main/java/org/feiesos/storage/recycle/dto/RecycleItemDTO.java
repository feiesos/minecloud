package org.feiesos.storage.recycle.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RecycleItemDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    Long id;

    String name;

    @JsonSerialize(using = ToStringSerializer.class)
    Long parentId;

    Boolean isDir;

    Long size;

    LocalDateTime createTime;

    LocalDateTime deleteTime;
}
