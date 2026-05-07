package com.boonya.business.trip.user.service;

import java.util.Collection;

public interface EmployeeCacheService {

    /**
     * 缓存员工姓名
     */
    void cacheEmployeeName(Long userId, String name);

    /**
     * 批量缓存员工姓名
     */
    void cacheEmployeeNames(Collection<Long> employeeIds);

    /**
     * 删除员工缓存
     */
    void removeEmployeeName(Long employeeId);
}
