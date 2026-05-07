package com.boonya.business.trip.common.models.user.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String roles;
    private String captcha;
}
