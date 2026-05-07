package com.boonya.business.trip.common.models.order.request;

import lombok.Data;

@Data
public class StandardQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String name;
}
