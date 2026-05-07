package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.user.mapper.EmployeeMapper;
import com.boonya.business.trip.user.service.EmployeeCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EmployeeCacheServiceImpl implements EmployeeCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmployeeMapper employeeMapper;

    private static final String EMPLOYEE_KEY_PREFIX = "cache:employee:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void cacheEmployeeName(Long employeeId, String employeeName) {
        if (employeeId == null || employeeName == null) {
            return;
        }
        String key = EMPLOYEE_KEY_PREFIX + employeeId;
        redisTemplate.opsForValue().set(key, employeeName, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void cacheEmployeeNames(Collection<Long> employeeIds) {
        if (CollectionUtils.isEmpty(employeeIds)) {
            return;
        }

        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Employee::getId, employeeIds);
        List<Employee> employees = employeeMapper.selectList(wrapper);

        for (Employee employee : employees) {
            cacheEmployeeName(employee.getId(), employee.getName());
        }
    }

    @Override
    public void removeEmployeeName(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        String key = EMPLOYEE_KEY_PREFIX + employeeId;
        redisTemplate.delete(key);
    }
}
