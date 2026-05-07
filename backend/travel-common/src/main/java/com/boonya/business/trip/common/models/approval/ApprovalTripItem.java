package com.boonya.business.trip.common.models.approval;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.Scene;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 申请审批单中的出行项
 */
@Data
public class ApprovalTripItem {
    private String uuid;                    // 生成UUID唯一记录

    private Scene tripType;                 // 出行类型：FLIGHT/HOTEL/TRAIN/CAR

    private String tripName;                // 出行项名称（如：北京到上海航班）

    private String departure;               // 出发地

    private String arrival;                 // 目的地

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;        // 开始时间（出发时间/入住时间）

    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;          // 结束时间（到达时间/离店时间）
}
