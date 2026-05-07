package com.boonya.business.trip.common.constant;

public enum IdentityType {
    /**
     * 身份证
     */
    ID_CARD("ID_CARD"),
    /**
     * 护照
     */
    PASSPORT("PASSPORT"),
    /**
     * 港澳台通行证
     */
    HONGKONG_MACAO_TAIWAN_CERTIFICATE("HONGKONG_MACAO_TAIWAN_CERTIFICATE");
    private String value;
    IdentityType(String value) {
        this.value = value;
    }
}
