package com.boonya.business.trip.common.constant;

public enum AuditStatus {
    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "通过"),
    REJECTED("REJECTED", "拒绝");
    private String value;
    private String description;

    AuditStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
