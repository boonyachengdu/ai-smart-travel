package com.boonya.business.trip.common.component.cache;

import java.util.Map;

public interface CacheService {

    /**
     * 根据用户 ID 获取用户名
     */
    String getUsernameById(Long userId);

    /**
     * 根据企业 ID 获取企业名称
     */
    String getCompanyNameById(Long companyId);

    /**
     * 根据部门 ID 获取部门名称
     */
    String getDepartmentNameById(Long deptId);

    /**
     * 根据员工 ID 获取员工名称
     */
    String getEmployeeNameById(Long employeeId);

    /**
     * 批量获取用户名称
     */
    Map<Long, String> getUsernamesByIds(java.util.Collection<Long> userIds);

    /**
     * 批量获取企业名称
     */
    Map<Long, String> getCompanyNamesByIds(java.util.Collection<Long> companyIds);

    /**
     * 批量获取部门名称
     */
    Map<Long, String> getDepartmentNamesByIds(java.util.Collection<Long> deptIds);

    /**
     * 批量获取员工名称
     */
    Map<Long, String> getEmployeeNamesByIds(java.util.Collection<Long> employeeIds);
}

