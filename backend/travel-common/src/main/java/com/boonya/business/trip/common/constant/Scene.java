package com.boonya.business.trip.common.constant;

import lombok.Getter;

@Getter
public enum Scene {
    /**
     * 问答咨询场景：商旅政策
     */
    QA("QA","问答资讯问题场景"),
    /**
     * 航班预定场景
     */
    FLIGHT("FLIGHT","航班预订场景"),
    /**
     * 酒店预定场景
     */
    HOTEL("HOTEL","酒店预订场景"),
    /**
     * 火车预定场景
     */
    TRAIN("TRAIN","火车预订场景"),
    /**
     * 用车预定场景
     */
    CAR("CAR","用车预订场景");
    private String value;
    private String desc;
    Scene(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
