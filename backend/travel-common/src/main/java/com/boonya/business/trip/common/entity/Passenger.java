package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.IdentityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户/乘客信息
 *
 * @description: 用户维护的乘客信息
 */
@Data
@TableName("passengers")
public class Passenger extends BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;// 姓名

    @TableField("gender")
    private String gender; // 性别

    @TableField("birthday")
    private String birthday; // 生日

    @TableField("phone")
    private String phone; // 手机

    @TableField("id_type")
    private IdentityType idType;// 证件类型

    @TableField("id_number")
    private String idNumber;// 证件号码

    @TableField("company_id")
    private Long companyId; // 公司ID

    @TableField("user_id")
    private Long userId; // 用户ID 谁添加谁可见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}
