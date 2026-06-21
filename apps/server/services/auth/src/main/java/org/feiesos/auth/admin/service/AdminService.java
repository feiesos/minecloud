package org.feiesos.auth.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.feiesos.auth.admin.dto.AdminDashboardVO;
import org.feiesos.auth.admin.dto.AdminPermissionVO;
import org.feiesos.auth.admin.dto.AdminRoleVO;
import org.feiesos.auth.admin.dto.AdminUserVO;
import org.feiesos.auth.entity.SysPermission;
import org.feiesos.auth.entity.SysRole;

import java.util.List;

public interface AdminService {

    void checkAdminAccess(Long userId);

    IPage<AdminUserVO> listUsers(int page, int size, String keyword);

    AdminUserVO getUserDetail(Long userId);

    void updateUser(Long targetUserId, String nickname, String email, Boolean enabled);

    void assignUserRoles(Long targetUserId, List<Long> roleIds);

    List<AdminRoleVO> listRoles();

    AdminRoleVO createRole(String code, String name);

    AdminRoleVO updateRole(Long roleId, String code, String name);

    void deleteRole(Long roleId);

    void assignRolePermissions(Long roleId, List<Long> permissionIds);

    List<AdminPermissionVO> listPermissions();

    AdminPermissionVO createPermission(String code, String name);

    AdminPermissionVO updatePermission(Long permissionId, String code, String name);

    void deletePermission(Long permissionId);

    AdminDashboardVO getDashboard();
}
