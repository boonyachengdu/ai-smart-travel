package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public abstract class BaseEntity {

    /**
     * 用户名（非持久化字段，从缓存读取）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 企业名称（非持久化字段，从缓存读取）
     */
    @TableField(exist = false)
    private String companyName;

    /**
     * 部门名称（非持久化字段，从缓存读取）
     */
    @TableField(exist = false)
    private String departmentName;

    /**
     * 申请人员工名称（非持久化字段，从缓存读取）
     */
    @TableField(exist = false)
    private String applicantEmployeeName;

    /**
     * 审批人员工名称（非持久化字段，从缓存读取）
     */
    @TableField(exist = false)
    private String approverEmployeeName;
}
