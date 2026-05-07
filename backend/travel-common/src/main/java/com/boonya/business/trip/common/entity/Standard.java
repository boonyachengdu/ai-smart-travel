package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 旅行标准（差标）实体类
 */
@Data
@TableName("standards")
public class Standard extends BaseEntity{

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;// 公司 ID

    @TableField("name")
    private String name; // 旅行标准名称

    @TableField("content")
    private String content;// 旅行标准内容 自定义/不规则差标说明

    @TableField("description")
    private String description;// 旅行标准描述

    @TableField("flight_max_price")
    private Double flightMaxPrice; // 机票最高价格（元）

    @TableField("hotel_max_price")
    private Double hotelMaxPrice; // 酒店最高价格（元/晚）

    @TableField("train_max_price")
    private Double trainMaxPrice; // 火车最高价格（元）

    @TableField("car_max_price")
    private Double carMaxPrice; // 用车最高价格（元）

    @TableField("month_max_price")
    private Double monthMaxPrice; // 每月上限价格（元）

    @TableField("year_max_price")
    private Double yearMaxPrice; // 每年上限价格（元）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除
}
