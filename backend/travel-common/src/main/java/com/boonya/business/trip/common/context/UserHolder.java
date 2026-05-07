package com.boonya.business.trip.common.context;

import com.boonya.business.trip.common.constant.RoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHolder {
    private String username;
    private Long userId;
    private Long companyId;
    private Long employeeId;
    private Long deptId;
    private String roles;

    private static final ThreadLocal<UserHolder> CONTEXT = new ThreadLocal<>();

    public static void set(UserHolder userContext) {
        CONTEXT.set(userContext);
    }

    public static UserHolder get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public boolean isSuperAdmin() {
        if (roles == null) {
            return false;
        }
        return roles.contains(RoleType.ROLE_SUPER_ADMIN.name());
    }

    public boolean isAdmin() {
        if (roles == null) {
            return false;
        }
        return roles.contains(RoleType.ROLE_ADMIN.name());
    }

    public boolean isAuditor() {
        if (roles == null) {
            return false;
        }
        return roles.contains(RoleType.ROLE_AUDITOR.name());
    }

    public boolean isUser() {
        if (roles == null) {
            return false;
        }
        return roles.contains(RoleType.ROLE_USER.name());
    }
}