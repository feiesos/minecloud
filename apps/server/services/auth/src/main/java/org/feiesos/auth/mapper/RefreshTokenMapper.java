package org.feiesos.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.feiesos.auth.entity.SysRefreshToken;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<SysRefreshToken> {

    @Select("SELECT * FROM sys_refresh_token WHERE token = #{token} AND revoked = false")
    SysRefreshToken findByToken(String token);

    @Select("SELECT * FROM sys_refresh_token WHERE token = #{token}")
    SysRefreshToken findByTokenIgnoreRevoked(String token);

    @Update("UPDATE sys_refresh_token SET revoked = true WHERE user_id = #{userId} AND revoked = false")
    int revokeAllByUserId(Long userId);

    @Delete("DELETE FROM sys_refresh_token WHERE expires_at < NOW()")
    int deleteExpired();
}
