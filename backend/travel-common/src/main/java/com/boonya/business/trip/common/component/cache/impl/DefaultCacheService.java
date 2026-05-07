package com.boonya.business.trip.common.component.cache.impl;

import com.boonya.business.trip.common.component.cache.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultCacheService implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_KEY_PREFIX = "cache:user:";
    private static final String COMPANY_KEY_PREFIX = "cache:company:";
    private static final String DEPT_KEY_PREFIX = "cache:dept:";

    private static final String EMPLOYEE_KEY_PREFIX = "cache:employee:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public String getUsernameById(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_KEY_PREFIX + userId;
        String username = (String) redisTemplate.opsForValue().get(key);
        if (username == null) {
            // TODO: 从数据库加载并缓存
        }
        return username;
    }

    @Override
    public String getCompanyNameById(Long companyId) {
        if (companyId == null) {
            return null;
        }
        String key = COMPANY_KEY_PREFIX + companyId;
        String companyName = (String) redisTemplate.opsForValue().get(key);
        if (companyName == null) {
            // TODO: 从数据库加载并缓存
        }
        return companyName;
    }

    @Override
    public String getDepartmentNameById(Long deptId) {
        if (deptId == null) {
            return null;
        }
        String key = DEPT_KEY_PREFIX + deptId;
        String deptName = (String) redisTemplate.opsForValue().get(key);
        if (deptName == null) {
            // TODO: 从数据库加载并缓存
        }
        return deptName;
    }

    @Override
    public String getEmployeeNameById(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        String key = EMPLOYEE_KEY_PREFIX + employeeId;
        String employeeName = (String) redisTemplate.opsForValue().get(key);
        if (employeeName == null) {
            // TODO: 从数据库加载并缓存
        }
        return employeeName;
    }

    @Override
    public Map<Long, String> getUsernamesByIds(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<String> keys = userIds.stream()
                .map(id -> USER_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, String> result = new HashMap<>();
        if (values != null) {
            Iterator<Long> idIter = userIds.iterator();
            Iterator<Object> valueIter = values.iterator();
            while (idIter.hasNext() && valueIter.hasNext()) {
                Long id = idIter.next();
                String value = (String) valueIter.next();
                if (value != null) {
                    result.put(id, value);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, String> getCompanyNamesByIds(Collection<Long> companyIds) {
        if (CollectionUtils.isEmpty(companyIds)) {
            return Collections.emptyMap();
        }
        List<String> keys = companyIds.stream()
                .map(id -> COMPANY_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, String> result = new HashMap<>();
        if (values != null) {
            Iterator<Long> idIter = companyIds.iterator();
            Iterator<Object> valueIter = values.iterator();
            while (idIter.hasNext() && valueIter.hasNext()) {
                Long id = idIter.next();
                String value = (String) valueIter.next();
                if (value != null) {
                    result.put(id, value);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, String> getDepartmentNamesByIds(Collection<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        List<String> keys = deptIds.stream()
                .map(id -> DEPT_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, String> result = new HashMap<>();
        if (values != null) {
            Iterator<Long> idIter = deptIds.iterator();
            Iterator<Object> valueIter = values.iterator();
            while (idIter.hasNext() && valueIter.hasNext()) {
                Long id = idIter.next();
                String value = (String) valueIter.next();
                if (value != null) {
                    result.put(id, value);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, String> getEmployeeNamesByIds(Collection<Long> employeeIds) {
        if (CollectionUtils.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        List<String> keys = employeeIds.stream()
                .map(id -> EMPLOYEE_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, String> result = new HashMap<>();
        if (values != null) {
            Iterator<Long> idIter = employeeIds.iterator();
            Iterator<Object> valueIter = values.iterator();
            while (idIter.hasNext() && valueIter.hasNext()) {
                Long id = idIter.next();
                String value = (String) valueIter.next();
                if (value != null) {
                    result.put(id, value);
                }
            }
        }
        return result;
    }
}

