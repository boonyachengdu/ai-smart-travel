package com.boonya.business.trip.common.models.channel;

import com.boonya.business.trip.common.utils.TimeCalcUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightSearchResponse implements SolutionName {
    private Integer index;
    private String flightNo;
    private String airline;
    private String departure;
    private String arrival;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;
    private String cabinClass;
    private String channel;
    private Integer availableSeats;

    @Override
    @JsonProperty("solutionName")
    public String getSolutionName() {
        String crossDayTime = TimeCalcUtil.calculateTimeDifference(departureTime, arrivalTime);
        return index + " " + flightNo + " - " + airline + " - " + cabinClass + " - ￥" + price + " - " + crossDayTime;
    }
}
