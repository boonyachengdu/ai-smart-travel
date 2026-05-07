package com.boonya.business.trip.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.boonya.business.trip.common.constant.PetType;
import com.boonya.business.trip.common.constant.Scene;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票信息实体类
 *
 * @Description 机票 火车票 （酒店、用车没有对应的数据）
 */
@Data
@TableName("tickets")
public class Ticket extends BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("journey_id")
    private Long journeyId;// 行程 ID（外键）

    @TableField("card_no")
    private String cardNo;//票号

    @TableField("travel_type")
    private Scene travelType;//票类型

    @TableField("departure")
    private String departure;//出发地

    @TableField("arrival")
    private String arrival;//目的地

    @TableField("departure_time")
    private LocalDateTime departureTime;//出发时间 入住时间

    @TableField("arrival_time")
    private LocalDateTime arrivalTime;//到达时间 离开时间

    @TableField("company_id")
    private Long companyId;  //公司ID

    @TableField("passenger_id")
    private Long passengerId; //客户或乘客ID  票和客户、乘客是一对一关系 一人一票

    @TableField("passenger_name")
    private String passengerName; //客户或乘客姓名

    @TableField("has_child")
    private Boolean hasChild; //是否有儿童

    @TableField("child_age")
    private Integer childAge;//儿童年龄 按年

    @TableField("has_baby")
    private Boolean hasBaby;//是否有婴儿

    @TableField("baby_age")
    private Integer babyAge;//婴儿年龄 按月

    @TableField("has_pet")
    private Boolean hasPet; //是否有宠物

    @TableField("pet_type")
    private PetType petType; //宠物类型

    @TableField("amount")
    private BigDecimal amount;// 总价

    @TableField("price")
    private BigDecimal price;// 单价

    @TableField("tax_fee")
    private BigDecimal taxFee;// 税费

    @TableField("insurance_fee")
    private BigDecimal insuranceFee; // 保险费

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;// 创建时间

    @TableField(value = "update_time", update = "CURRENT_TIMESTAMP", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;// 更新时间

    @TableLogic
    private Integer deleted = 0;// 逻辑删除

}
