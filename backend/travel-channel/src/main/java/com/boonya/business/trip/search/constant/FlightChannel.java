package com.boonya.business.trip.search.constant;

public enum FlightChannel {
    HANG_BAN_GUAN_JIA("HANG_BAN_GUAN_JIA", "航班管家"),
    CTRIP("CTRIP", "携程"),
    QUNAR("QUNAR", "去哪儿"),
    TONGCHENG("TONGCHENG", "同程"),
    FEIZHU("FEIZHU", "飞猪");

    private String code;
    private String desc;

    FlightChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
