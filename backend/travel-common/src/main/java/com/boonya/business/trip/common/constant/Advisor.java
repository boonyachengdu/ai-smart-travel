package com.boonya.business.trip.common.constant;

public enum Advisor {
    /**
     * 商旅政策
     */
    POLICY("POLICY"),
    /**
     * 差旅规划
     */
    PLANNING("PLANNING"),
    /**
     * 成本计算
     */
    COSTS("COSTS"),
    /**
     * 航班建议
     */
    FLIGHT("FLIGHT"),
    /**
     * 酒店建议
     */
    HOTEL("HOTEL"),
    /**
     * 火车建议
     */
    TRAIN("TRAIN"),
    /**
     * 用车建议
     */
    CAR("CAR"),
    /**
     * 审批建议
     */
    APPROVAL("APPROVAL"),
    /**
     * 报销建议
     */
    REIMBURSEMENT("REIMBURSEMENT"),
    /**
     * 安全建议
     */
    SAFETY("SAFETY"),
    ;
    private String value;
    Advisor(String value) {
        this.value = value;
    }
}
