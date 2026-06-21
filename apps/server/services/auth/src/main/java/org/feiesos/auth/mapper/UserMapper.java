package org.feiesos.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.feiesos.auth.entity.SysUser;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = false")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = false")
    SysUser findByEmail(String email);

    @Select("SELECT * FROM sys_user WHERE verification_token = #{token} AND deleted = false")
    SysUser findByVerificationToken(String token);

    @Select("SELECT * FROM sys_user WHERE reset_password_token = #{token} AND deleted = false")
    SysUser findByResetPasswordToken(String token);
}

