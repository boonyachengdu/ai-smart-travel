package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@TableName("chat_messages")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;// 会话 ID

    @TableField("role")
    private String role;// 角色：user / assistant / system

    @TableField("content")
    private String content;// 消息内容

    @TableField("message_type")
    private String messageType;// 消息类型 text / file / order_json / policy_check

    @TableField("file_url")
    private String fileUrl;// 文件地址

    @TableField("metadata")
    private String metadata; // JSONB 可映射为 String 或用 @Type(JsonBinaryType.class)

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}
