package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.constant.SessionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

// ... existing code ...
@Data
@TableName("chat_sessions")
public class ChatSession extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId; // 用户 ID

    @TableField("company_id")
    private Long companyId; //公司 ID

    @TableField("scene")
    private Scene scene;    //场景

    @TableField("title")
    private String title;   //会话标题

    @TableField("status")
    private SessionStatus status;// 会话状态  active/closed/archived

    @TableField("current_stage")
    private String currentStage; // 当前对话阶段：INIT/INTENT_RECOGNITION/PARAMETER_COLLECTION/...

    @TableField("collected_params")
    private String collectedParams; // JSON 格式存储已收集的参数（不展示给用户）

    @TableField(value = "last_active_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastActiveTime;//最后活跃时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}
