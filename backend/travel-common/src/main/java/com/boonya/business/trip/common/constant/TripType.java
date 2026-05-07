package com.boonya.business.trip.common.constant;

public enum TripType {
    ONE_WAY("ONE_WAY", "单程"),
    ROUND_TRIP("ROUND_TRIP", "往返"),
    MULTI_STOP("MULTI_STOP", "多程"),
    OUTBOUND("OUTBOUND", "去程"),
    RETURN("RETURN", "返程");
    private String code;
    private String desc;

    TripType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
