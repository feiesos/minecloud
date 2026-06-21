package org.feiesos.auth.admin.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDashboardVO {
    long totalUsers;
    long totalFiles;
    long totalFileSize;
    long totalRecycled;
    long totalRoles;
    long totalPermissions;
}
