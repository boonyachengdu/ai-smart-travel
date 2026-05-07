package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.Advisor;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词实体类
 */
@Data
@TableName("prompts")
public class Prompts extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;// 提示词名称

    @TableField("advisor")
    private Advisor advisor;// 商旅顾问

    @TableField("prompt")
    private String prompt;// 提示词内容

    @TableField("description")
    private String description;// 提示词描述

    @TableField("enabled")
    private Boolean enabled;// 是否启用

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    @TableField("company_id")
    private Long companyId;// 公司ID

    @TableField("user_id")
    private Long userId; // 用户ID
}
