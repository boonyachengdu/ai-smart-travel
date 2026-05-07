package com.boonya.business.trip.common.constant;

public enum RoleType {
    /**
     * 超级管理员
     */
    ROLE_SUPER_ADMIN("ROLE_SUPER_ADMIN"),
    /**
     * 管理员
     */
    ROLE_ADMIN("ROLE_ADMIN"),
    /**
     * 审核员
     */
    ROLE_AUDITOR("ROLE_AUDITOR"),
    /**
     * 普通用户
     */
    ROLE_USER("ROLE_USER"),
    /**
     * 游客
     */
    ROLE_GUEST("ROLE_GUEST");

    private String role;

    RoleType(String role) {
        this.role = role;
    }
}
