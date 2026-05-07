package com.boonya.business.trip.common.models.channel;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TrainSearchRequest extends TravelBaseRequest{
    private String departure;
    private String arrival;
    private LocalDateTime departureDate;
    private Integer adultCount;
    private Integer childCount;
    private String seatType;
}
