package com.boonya.business.trip.common.constant;

public enum SessionStatus {
    /**
     * 激活状态
     */
    ACTIVE("active"),
    /**
     * 关闭状态
     */
    CLOSED("closed"),
    /**
     * 休眠状态
     */
    INACTIVE("inactive");
    private String status;
    SessionStatus(String status) {
        this.status = status;
    }
}
