package com.boonya.business.trip.common.models.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private String orderType;
    private BigDecimal amount;
    private String status;
    private String departure;
    private String arrival;
    private LocalDateTime departureTime;
    private LocalDateTime createTime;
}
