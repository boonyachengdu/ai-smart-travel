package com.boonya.business.trip.search.constant;

public enum HotelChannel {
    JIN_JIANG_HOTEL("JIN_JIANG_HOTEL", "锦江酒店"),
    RU_JIA_HOTEL("RU_JIA_HOTEL", "如家酒店"),
    HANTING_HOTEL("HANTING_HOTEL", "汉庭酒店"),
    JI_HOTEL("JI_HOTEL", "全季酒店"),
    ATOUR_HOTEL("ATOUR_HOTEL", "亚朵酒店"),
    HILTON_HOTEL("HILTON_HOTEL", "希尔顿酒店"),
    MARRIOTT_HOTEL("MARRIOTT_HOTEL", "万豪酒店");

    private String code;
    private String desc;

    HotelChannel(String code, String desc) {
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
