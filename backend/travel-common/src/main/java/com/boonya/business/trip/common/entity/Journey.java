package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.TripType;
import com.boonya.business.trip.common.json.TicketListTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单行程信息
 * @description: 一个订单可以有多段行程，一个行程可以有多张票
 */
@Data
@TableName("journeys")
public class Journey extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;                 // 订单 ID（外键）

    @TableField("company_id")
    private Long companyId;               // 公司ID

    @TableField("flight_no")
    private String flightNo;              // 航班号

    @TableField("train_no")
    private String trainNo;               // 火车车次

    @TableField("hotel_name")
    private String hotelName;             // 酒店名称

    @TableField("car_no")
    private String carNo;                 // 车牌号

    @TableField("departure")
    private String departure;         // 出发地

    @TableField("arrival")
    private String arrival;           // 目的地

    @TableField("departure_time")
    private LocalDateTime departureTime;  // 出发时间 入住时间

    @TableField("arrival_time")
    private LocalDateTime arrivalTime; // 到达时间 离开时间

    @TableField(typeHandler = TicketListTypeHandler.class)
    private List<Ticket> tickets; // 票列表信息 一个行程可以有多张票

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

    @TableField("trip_type")
    private TripType tripType; // 行程类型

    @TableField("airline")
    private String airline;   //  航空公司

    @TableField("cabin_class")
    private String cabinClass; // 仓位等级
}
