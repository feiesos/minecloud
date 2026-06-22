package org.feiesos.share.dto;

import lombok.Data;

/**
 * 访问分享请求 DTO（密码验证）
 */
@Data
public class AccessShareRequest {

    /** 访问密码（如果分享需要密码） */
    private String password;
}