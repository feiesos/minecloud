package org.feiesos.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper {

    @Select("""
            SELECT COUNT(*) FROM sys_user_role ur
            JOIN sys_role r ON ur.role_id = r.id AND r.deleted = false
            JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            JOIN sys_permission p ON rp.permission_id = p.id AND p.deleted = false
            JOIN sys_user u ON ur.user_id = u.id AND u.deleted = false AND u.enabled = true
            WHERE ur.user_id = #{userId} AND p.code = #{permissionCode}
            """)
    int countPermission(@Param("userId") Long userId, @Param("permissionCode") String permissionCode);
}
