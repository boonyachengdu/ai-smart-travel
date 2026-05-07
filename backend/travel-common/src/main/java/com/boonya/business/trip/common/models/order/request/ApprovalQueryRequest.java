package com.boonya.business.trip.common.models.order.request;

import lombok.Data;

@Data
public class ApprovalQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String orderNo;
    private String status;
    private String applicantEmployeeName;
    private String approverEmployeeName;
}
