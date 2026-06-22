package org.feiesos.share.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 创建分享请求 DTO
 */
@Data
@Builder
public class CreateShareRequest {

    /** 文件/目录ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileNodeId;

    /** 访问密码（可选，为空表示公开分享） */
    private String accessPassword;

    /** 过期时间（可选，为空表示永不过期） */
    private OffsetDateTime expireAt;

    /** 最大下载次数（可选，-1表示无限制） */
    private Integer maxDownloads;

    /** 分享备注 */
    private String remark;
}