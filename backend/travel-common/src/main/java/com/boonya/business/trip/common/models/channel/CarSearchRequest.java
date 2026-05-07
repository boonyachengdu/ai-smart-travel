package com.boonya.business.trip.common.models.channel;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CarSearchRequest extends TravelBaseRequest {
    private String city;
    private LocalDateTime useTime;
    private String destination;
    private String carType;
    private Integer passengerCount;
}