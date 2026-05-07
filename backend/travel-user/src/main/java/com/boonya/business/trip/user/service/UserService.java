package com.boonya.business.trip.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.vo.UserLoginVO;

public interface UserService extends IService<User> {
    User findByUsername(String username);

    Response<UserLoginVO> login(String username, String password);

    User register(String username, String password, String email, String phone, String roles);

    Long countUsers();
}
