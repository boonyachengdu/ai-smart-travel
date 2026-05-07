package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.json.ApprovalTripItemListTypeHandler;
import com.boonya.business.trip.common.models.approval.ApprovalTripItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("approvals")
public class Approval extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;           // 订单号（如果是关联订单） 可关联多个订单号

    @TableField("company_id")
    private Long companyId;        // 公司 ID

    @TableField("applicant_id")
    private Long applicantId;       // 申请人 (员工 ID)

    @TableField("approver_id")
    private Long approverId;        // 审批人 (员工 ID)

    @TableField("status")
    private AuditStatus status;     // 审批状态：待审批/通过/拒绝

    @TableField("reason")
    private String reason;          // 出差事由

    @TableField("remark")
    private String remark;          // 审批意见/备注

    @TableField(value = "trip_items", typeHandler = ApprovalTripItemListTypeHandler.class)
    private List<ApprovalTripItem> tripItems;  // 出行项列表（JSON 存储）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted = 0;
}
