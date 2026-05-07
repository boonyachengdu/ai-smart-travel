package com.boonya.business.trip.common.models.channel;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HotelSearchRequest extends TravelBaseRequest{
    private String city;
    private LocalDateTime checkInDate;
    private LocalDateTime checkOutDate;
    private Integer roomCount;
    private Integer adultCount;
    private String starLevel;
}
