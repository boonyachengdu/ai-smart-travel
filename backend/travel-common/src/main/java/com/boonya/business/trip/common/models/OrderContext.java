package com.boonya.business.trip.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单数据模型（用于合规检查）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderContext {

    /**
     * 订单类型：FLIGHT/HOTEL/TRAIN/CAR
     */
    private String orderType;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 出发地
     */
    private String departure;

    /**
     * 目的地
     */
    private String arrival;

    /**
     * 出发日期
     */
    private String departureDate;

    /**
     * 返程日期
     */
    private String returnDate;

    /**
     * 舱位等级/房型等级
     */
    private String classLevel;

    /**
     * 乘客人数
     */
    private Integer passengerCount;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 部门名称
     */
    private String departmentName;
}
