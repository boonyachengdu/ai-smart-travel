package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.Scene;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RAG 文件上传记录实体
 * 用于记录用户或企业上传的知识文件，支持用户隔离和企业隔离
 */
@Data
@TableName("rag_files")
public class RagFile extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene")
    private Scene scene; // 场景

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 存储路径（OSS/MinIO key）
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * MIME 类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 状态：ENABLED / DISABLED / DELETED
     */
    @TableField("status")
    private String status = "ENABLED";

    /**
     * 上传用户ID（个人文件必填）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 所属企业ID（企业文件必填）
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 文件描述/备注
     */
    @TableField("description")
    private String description;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}