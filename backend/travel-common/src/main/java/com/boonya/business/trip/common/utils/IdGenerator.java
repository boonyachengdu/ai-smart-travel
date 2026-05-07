package com.boonya.business.trip.common.utils;

import java.util.UUID;

public class IdGenerator {

    public static String getUuid(){
        String uuid = UUID.randomUUID().toString();
        return uuid.replaceAll("-", "");
    }
}
