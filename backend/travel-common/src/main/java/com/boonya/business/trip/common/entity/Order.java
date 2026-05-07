package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.constant.OrderStatus;
import com.boonya.business.trip.common.constant.PayType;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.json.JourneyListTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体（适配 PostgreSQL）
 * @Description: 抽象出订单对应的行程信息
 */
@Data
@TableName("orders")
public class Order extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;           // 公司ID

    @TableField("order_no")
    private String orderNo;           // 订单号（数据库已建唯一约束，业务层需保证唯一）

    @TableField("user_id")
    private Long userId;              // 用户ID

    @TableField("username")
    private String username;          // 用户名（冗余）

    @TableField("order_type")
    private Scene orderType;          // 订单类型

    @TableField("amount")
    private BigDecimal amount;        // 金额

    @TableField("status")
    private OrderStatus status;            // 订单状态：草稿/渠道下单中/待支付/已支付/已完成/已取消/退款中/已退款

    @TableField("audit_status")
    private AuditStatus auditStatus;      // 审核状态：待审批/通过/拒绝

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    @TableField("pay_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime payTime;        // 支付时间

    @TableField("pay_type")
    private PayType payType;              // 支付方式

    @TableField("refundable")
    private Boolean refundable;           // 是否可退改

    @TableField("refund_amount")
    private BigDecimal refundAmount;      // 退款金额

    @TableField(typeHandler = JourneyListTypeHandler.class)
    private List<Journey> journey;        // 行程列表 一个订单可以有多段行程

    @TableField("approval_id")
    private Long approvalId;// 申请单ID

    @TableField("order_requirements")
    private String orderRequirements;// 订单要求

    @TableField("remark")
    private String remark;// 备注信息
}