package org.feiesos.share.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文件分享实体（对应 file_share 表）
 */
@Data
@TableName("file_share")
public class FileShare {

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 分享令牌（短链接标识） */
    private String shareToken;

    /** 被分享的文件/目录ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileNodeId;

    /** 分享者用户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;

    /** 访问密码（为空表示公开分享） */
    private String accessPassword;

    /** 过期时间（为空表示永不过期） */
    private OffsetDateTime expireAt;

    /** 允许下载次数（-1表示无限制） */
    private Integer maxDownloads;

    /** 已下载次数 */
    private Integer downloadCount;

    /** 分享备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 逻辑删除标记 */
    @TableLogic
    private Boolean deleted;
}