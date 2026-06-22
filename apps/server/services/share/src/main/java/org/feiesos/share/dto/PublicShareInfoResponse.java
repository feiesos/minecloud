package org.feiesos.share.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 公开分享信息响应 DTO
 */
@Data
@Builder
public class PublicShareInfoResponse {

    /** 分享令牌 */
    private String shareToken;

    /** 文件/目录ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileNodeId;

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
}