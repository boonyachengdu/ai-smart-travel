package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.RoleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息实体类
 *
 * @description: 系统登录用户信息
 */
@Data
@TableName("users")
public class User extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;     // 唯一键约束

    @TableField("password")
    private String password;     // 加密存储

    @TableField("email")
    private String email;       // 邮箱

    @TableField("phone")
    private String phone;       //  手机

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    @TableField("roles")
    private String roles; // 角色 多个用逗号分隔

    @TableField("company_id")
    private Long companyId;  // 企业ID 该字段不真实存在，仅用于缓存
}