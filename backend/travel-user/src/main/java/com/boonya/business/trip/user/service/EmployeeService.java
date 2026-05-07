package com.boonya.business.trip.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.entity.Employee;

public interface EmployeeService extends IService<Employee> {
    Employee getActiveEmployeeByUserId(Long userId);
}
