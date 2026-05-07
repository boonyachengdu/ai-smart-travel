package com.boonya.business.trip.common.models.dialog;

import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.entity.Passenger;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单需求-最终渠道出票须符合要求
 */
@Data
public class OrderRequirements implements Serializable {
    /**
     * 订单号// 创单成功后返回（往返会有两个订单号）
     */
    private String orderNo;

    /**
     * ------------------------AI 封装字段----------------------
     */

    /**
     * 选项：航班、酒店、火车车次(用户回答后进行选择)，从上下文中给出5个以内的最优解
     */
    private String selected;

    /**
     * 订单类型
     */
    private String orderType;
    /**
     * 出发城市
     */
    private String departureCity;
    /**
     * 到达城市
     */
    private String arrivalCity;
    /**
     * 出发时间
     */
    private String departureDate;
    /**
     * 抵达时间
     */
    private String arrivalDate;
    /**
     * 返回时间
     */
    private String returnDate;
    /**
     * 乘客、客户信息
     */
    private List<Passenger> passengers;
    /**
     * 预算
     */
    private Double budget;
    /**
     * 特殊要求
     */
    private String specialRequirements;

    /**
     * ----------------------------------机票字段---------
     */
    private String flightNo;
    private String airline;
    private String cabinClass;

}
