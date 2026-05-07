package com.boonya.business.trip.common.models.user.vo;

import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.common.entity.User;
import lombok.Data;

@Data
public class UserLoginVO {

    private String token;
    private User user;
    private Employee employee;
}
