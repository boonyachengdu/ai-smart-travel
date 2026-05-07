package com.boonya.business.trip.common.models.dialog;

import lombok.Data;

@Data
public class SessionQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private Long userId;
    private Long companyId;
    private String scene;
    private String status;
}
