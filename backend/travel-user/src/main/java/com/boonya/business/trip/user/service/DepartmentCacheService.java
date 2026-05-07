package com.boonya.business.trip.user.service;

import java.util.Collection;

public interface DepartmentCacheService {

    /**
     * 缓存部门名称
     */
    void cacheDepartmentName(Long deptId, String deptName);

    /**
     * 批量缓存部门名称
     */
    void cacheDepartmentNames(Collection<Long> deptIds);

    /**
     * 删除部门缓存
     */
    void removeDepartmentCache(Long deptId);
}
