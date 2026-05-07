package com.boonya.business.trip.common.models.channel;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TravelBaseRequest {
    /**
     * 渠道
     */
    private String channel;
    /**
     * 最大预算
     */
    private BigDecimal maxBudget;
    /**
     * 最小预算
     */
    private BigDecimal minBudget;

    /**
     * 扩展字段
     */
    private JSONObject extend;
}
