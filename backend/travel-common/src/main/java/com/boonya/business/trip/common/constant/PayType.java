package com.boonya.business.trip.common.constant;

public enum PayType {
    /**
     * 公司：授信支付
     */
    COMPANY_CREDIT("COMPANY_CREDIT"),
    /**
     * 个人：支付宝
     */
    PERSONAL_ALIPAY("PERSONAL_ALIPAY"),
    /**
     * 个人：微信
     */
    PERSONAL_WECHAT("PERSONAL_WECHAT");
    private String value;

    PayType(String value) {
        this.value = value;
    }
}
