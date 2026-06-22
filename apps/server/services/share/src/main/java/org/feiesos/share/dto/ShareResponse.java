package org.feiesos.share.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 分享链接响应 DTO
 */
@Data
@Builder
public class ShareResponse {

    /** 分享ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 分享令牌 */
    private String shareToken;

    /** 文件/目录ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileNodeId;

    /** 分享者用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;

    /** 是否需要密码 */
    private Boolean needPassword;

    /** 过期时间 */
    private OffsetDateTime expireAt;

    /** 是否已过期 */
    private Boolean expired;

    /** 最大下载次数 */
    private Integer maxDownloads;

    /** 已下载次数 */
    private Integer downloadCount;

    /** 分享备注 */
    private String remark;

    /** 创建时间 */
    private OffsetDateTime createdAt;

    /** 分享链接URL */
    private String shareUrl;
}