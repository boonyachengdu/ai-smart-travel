package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织机构管理实体类
 */
@Data
@TableName("departments")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("parent_id")
    private Long parentId; // 父级部门ID 0表示根节点

    @TableField("company_id")
    private Long companyId; // 公司ID

    @TableField("name")
    private String name;// 部门名称

    @TableField("description")
    private String description;// 部门描述

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}
