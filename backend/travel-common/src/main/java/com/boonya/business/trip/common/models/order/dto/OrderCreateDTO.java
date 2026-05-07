package com.boonya.business.trip.common.models.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreateDTO {
    private String orderType;
    private BigDecimal amount;
    private String departure;
    private String arrival;
    private LocalDateTime departureTime;
    // 其他字段根据需要扩展
}