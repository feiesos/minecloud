package org.feiesos.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String email;

    private String avatar;

    private Boolean enabled;

    private String verificationToken;

    private java.time.OffsetDateTime verifiedAt;

    private java.time.OffsetDateTime verificationTokenExpireAt;

    private String resetPasswordToken;

    private java.time.OffsetDateTime resetPasswordTokenExpireAt;

    @TableLogic
    private Boolean deleted;
}
