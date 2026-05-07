package com.boonya.business.trip.common.models.channel;

import com.boonya.business.trip.common.utils.TimeCalcUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HotelSearchResponse  implements SolutionName{
    private Integer index;
    private String hotelName;
    private String address;
    private String starLevel;
    private LocalDateTime checkInDate;
    private LocalDateTime checkOutDate;
    private BigDecimal price;
    private String channel;
    private Integer availableRooms;
    private String amenities;

    @Override
    @JsonProperty("solutionName")
    public String getSolutionName() {
        String crossDayTime = TimeCalcUtil.calculateTimeDifference(checkInDate, checkOutDate);
        return index + " " + hotelName + " - " + address + " - " + starLevel + " - ￥" + price + " - " + crossDayTime;
    }
}
