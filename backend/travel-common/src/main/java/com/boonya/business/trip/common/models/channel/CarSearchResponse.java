package com.boonya.business.trip.common.models.channel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CarSearchResponse {
    private Integer index;
    private String carId;
    private String carType;
    private String carName;
    private String driverName;
    private String driverPhone;
    private String licensePlate;
    private BigDecimal price;
    private LocalDateTime useTime;
    private String pickupAddress;
    private String destination;
    private Integer passengerCapacity;
    private String description;
}