package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.user.mapper.EmployeeMapper;
import com.boonya.business.trip.user.service.EmployeeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public Employee getActiveEmployeeByUserId(Long userId) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getUserId, userId);
        wrapper.eq(Employee::getDeleted, 0);
        wrapper.eq(Employee::getIsPrimary, true);
        return employeeMapper.selectOne(wrapper);
    }
}
