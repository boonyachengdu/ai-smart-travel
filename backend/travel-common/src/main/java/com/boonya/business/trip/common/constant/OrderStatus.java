package com.boonya.business.trip.common.constant;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 渠道下单中（正在向供应商/渠道下单）
     */
    ORDERING("ORDERING", "下单中"),

    /**
     * 待支付（订单已创建，等待支付）
     */
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),

    /**
     * 已支付（支付成功）
     */
    PAID("PAID", "已支付"),

    /**
     * 已完成（行程结束，订单完成）
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已取消（用户主动取消或审核不通过）
     */
    CANCELLED("CANCELLED", "已取消"),

    /**
     * 退款中（申请退款处理中）
     */
    REFUNDING("REFUNDING", "退款中"),

    /**
     * 已退款（退款完成）
     */
    REFUNDED("REFUNDED", "已退款");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 获取枚举
     */
    public static OrderStatus valueOfCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为终态（不可再变更的状态）
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * 判断是否可取消
     */
    public boolean canBeCancelled() {
        return this == PENDING_PAYMENT || this == PAID;
    }

    /**
     * 判断是否可退款
     */
    public boolean canBeRefunded() {
        return this == PAID || this == COMPLETED;
    }
}
