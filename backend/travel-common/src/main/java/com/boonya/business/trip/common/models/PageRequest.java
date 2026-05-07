package com.boonya.business.trip.common.models;

import lombok.Data;

@Data
public class PageRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String sort;
    private String order;
}
