package com.boonya.business.trip.common.models.user.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
