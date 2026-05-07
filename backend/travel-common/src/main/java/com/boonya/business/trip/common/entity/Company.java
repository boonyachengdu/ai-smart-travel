package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业实体类实体类
 *
 * @description: 只有系统admin可以添加企业，添加企业的时候自动创建企业对应账号信息
 */
@Data
@TableName("company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;//公司名称

    @TableField("description")
    private String description;// 描述

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    @TableField("auto_approve")
    private Boolean autoApprove;// 是否自动审批：0-否 1-是

    @TableField("user_name")
    private String userName;// 用户名 企业管理账号

    @TableField("password")
    private String password;// 密码  加密串

    @TableField("over_amount_must_audit")
    private BigDecimal overAmountMustAudit;// 金额超过多少必须审批
}
