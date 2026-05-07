package com.boonya.business.trip.common.models.order.request;

import lombok.Data;

@Data
public class OrderQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String orderNo;
    private String orderType;   // FLIGHT / HOTEL / TRAIN / CAR
    private String status;
    private String auditStatus;
    private Long userId;
    private Long companyId;
}
