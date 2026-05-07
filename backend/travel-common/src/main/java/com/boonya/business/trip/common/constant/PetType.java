package com.boonya.business.trip.common.constant;

/**
 * @ClassName: PetType
 */
public enum PetType {
    DOG("DOG", "狗"),
    CAT("CAT", "猫"),
    BIRD("BIRD", "鸟"),
    OTHER("OTHER", "其他");
    private String code;
    private String name;
    PetType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
