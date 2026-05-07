package com.boonya.business.trip.search.constant;

public enum TrainChannel {
    GAO_TIE_GUAN_JIA("GAO_TIE_GUAN_JIA", "高铁管家"),
    CTRIP("CTRIP", "携程"),
    QUNAR("QUNAR", "去哪儿"),
    TONGCHENG("TONGCHENG", "同程"),
    OFFICIAL_12306("OFFICIAL_12306", "12306 官网");

    private String code;
    private String desc;

    TrainChannel(String code, String desc) {
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
