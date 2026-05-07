package com.boonya.business.trip.common.models.user.request;

import lombok.Data;

@Data
public class UserQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private String phone;
    private String email;
}
