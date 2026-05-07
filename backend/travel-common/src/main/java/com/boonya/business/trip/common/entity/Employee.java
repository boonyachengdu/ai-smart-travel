package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("employees")
public class Employee extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;//公司ID

    @TableField("department_id")
    private Long departmentId; //部门ID

    @TableField("user_id")
    private Long userId;  // 用户ID

    // 补充业务字段
    @TableField("name")
    private String name;            // 姓名

    @TableField("position")
    private String position;        // 职位

    @TableField("employee_no")
    private String employeeNo;      // 工号

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("entry_date")
    private Date entryDate;         // 入职日期

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    // 切换企业所用，必有一个默认公司挂靠，最新的设为true，以前的设为false
    @TableField("is_primary")
    private Boolean isPrimary;  // 是否为默认公司

}
