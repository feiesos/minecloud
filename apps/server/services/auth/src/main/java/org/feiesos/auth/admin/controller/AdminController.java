package org.feiesos.auth.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.feiesos.auth.admin.dto.AdminDashboardVO;
import org.feiesos.auth.admin.dto.AdminPermissionVO;
import org.feiesos.auth.admin.dto.AdminRoleVO;
import org.feiesos.auth.admin.dto.AdminUserVO;
import org.feiesos.auth.admin.service.AdminService;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.result.R;
import org.feiesos.common.security.JwtClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public R<AdminDashboardVO> dashboard() {
        adminService.checkAdminAccess(getCurrentUserId());
        return R.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public R<IPage<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        adminService.checkAdminAccess(getCurrentUserId());
        return R.ok(adminService.listUsers(page, size, keyword));
    }

    @GetMapping("/users/{id}")
    public R<AdminUserVO> getUser(@PathVariable Long id) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            return R.ok(adminService.getUserDetail(id));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/users/{id}")
    public R<String> updateUser(@PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            String nickname = (String) body.get("nickname");
            String email = (String) body.get("email");
            Boolean enabled = body.get("enabled") != null ? Boolean.valueOf(body.get("enabled").toString()) : null;
            adminService.updateUser(id, nickname, email, enabled);
            return R.ok("更新成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/users/{id}/roles")
    public R<String> assignUserRoles(@PathVariable Long id,
                                      @RequestBody Map<String, List<Long>> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            adminService.assignUserRoles(id, body.get("roleIds"));
            return R.ok("角色分配成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/roles")
    public R<List<AdminRoleVO>> listRoles() {
        adminService.checkAdminAccess(getCurrentUserId());
        return R.ok(adminService.listRoles());
    }

    @PostMapping("/roles")
    public R<AdminRoleVO> createRole(@RequestBody Map<String, String> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            String code = body.get("code");
            String name = body.get("name");
            if (code == null || code.isBlank()) {
                return R.fail("角色编码不能为空");
            }
            if (name == null || name.isBlank()) {
                return R.fail("角色名称不能为空");
            }
            return R.ok(adminService.createRole(code.trim(), name.trim()));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/roles/{id}")
    public R<AdminRoleVO> updateRole(@PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            String code = body.get("code");
            String name = body.get("name");
            return R.ok(adminService.updateRole(id, code, name));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @DeleteMapping("/roles/{id}")
    public R<String> deleteRole(@PathVariable Long id) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            adminService.deleteRole(id);
            return R.ok("删除成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/roles/{id}/permissions")
    public R<String> assignRolePermissions(@PathVariable Long id,
                                            @RequestBody Map<String, List<Long>> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            adminService.assignRolePermissions(id, body.get("permissionIds"));
            return R.ok("权限分配成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/permissions")
    public R<List<AdminPermissionVO>> listPermissions() {
        adminService.checkAdminAccess(getCurrentUserId());
        return R.ok(adminService.listPermissions());
    }

    @PostMapping("/permissions")
    public R<AdminPermissionVO> createPermission(@RequestBody Map<String, String> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            String code = body.get("code");
            String name = body.get("name");
            if (code == null || code.isBlank()) {
                return R.fail("权限编码不能为空");
            }
            if (name == null || name.isBlank()) {
                return R.fail("权限名称不能为空");
            }
            return R.ok(adminService.createPermission(code.trim(), name.trim()));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/permissions/{id}")
    public R<AdminPermissionVO> updatePermission(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            String code = body.get("code");
            String name = body.get("name");
            return R.ok(adminService.updatePermission(id, code, name));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    @DeleteMapping("/permissions/{id}")
    public R<String> deletePermission(@PathVariable Long id) {
        adminService.checkAdminAccess(getCurrentUserId());
        try {
            adminService.deletePermission(id);
            return R.ok("删除成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未认证");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtClaims claims) {
            return claims.getUserId();
        }
        throw new BusinessException(401, "未认证");
    }
}
