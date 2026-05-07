package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.entity.Department;
import com.boonya.business.trip.user.mapper.DepartmentMapper;
import com.boonya.business.trip.user.service.DepartmentCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DepartmentCacheServiceImpl implements DepartmentCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DepartmentMapper departmentMapper;

    private static final String DEPT_KEY_PREFIX = "cache:dept:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void cacheDepartmentName(Long deptId, String deptName) {
        if (deptId == null || deptName == null) {
            return;
        }
        String key = DEPT_KEY_PREFIX + deptId;
        redisTemplate.opsForValue().set(key, deptName, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void cacheDepartmentNames(Collection<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return;
        }

        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Department::getId, deptIds);
        List<Department> departments = departmentMapper.selectList(wrapper);

        for (Department department : departments) {
            cacheDepartmentName(department.getId(), department.getName());
        }
    }

    @Override
    public void removeDepartmentCache(Long deptId) {
        if (deptId == null) {
            return;
        }
        String key = DEPT_KEY_PREFIX + deptId;
        redisTemplate.delete(key);
    }
}
