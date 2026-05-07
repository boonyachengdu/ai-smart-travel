package com.boonya.business.trip.common.models.user.request;

import lombok.Data;

@Data
public class CompanyQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String name;
    private String userName;
}
