package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.Scene;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政策文档实体类（用于 RAG 矢量检索）
 *
 * @description: 存储企业/部门的政策文档，支持数据隔离和向量检索
 */
@Data
@TableName("policy_documents")
public class PolicyDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId; // 企业 ID（必填，用于数据隔离）

    @TableField("dept_id")
    private Long deptId; // 部门 ID（可选，null 表示公司级通用政策）

    @TableField("content")
    private String content; // 政策内容

    @TableField("embedding")
    private String embedding; // 向量数据（PostgreSQL vector 类型，用 String 存储）

    @TableField("metadata")
    private String metadata; // JSONB 元数据

    @TableField("scene")
    private Scene scene; // 政策场景（如：TRAIN/FLIGHT/TRAIN/CAR/QA）

    @TableField("enabled")
    private Boolean enabled; // 是否启用

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime; // 更新时间
}
