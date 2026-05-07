package com.boonya.business.trip.feign.clients;


import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.models.channel.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "travel-channel",
        path = "/channel",
        configuration = FeignClientConfig.class
)
public interface ChannelFeignClient {

    @PostMapping("/mock/flight/search")
    public Response<List<FlightSearchResponse>> searchFlights(@RequestBody FlightSearchRequest req);

    @PostMapping("/mock/hotel/search")
    public Response<List<HotelSearchResponse>> searchHotels(@RequestBody HotelSearchRequest req);

    @PostMapping("/mock/train/search")
    public Response<List<TrainSearchResponse>> searchTrains(@RequestBody TrainSearchRequest req);

    @PostMapping("/mock/car/search")
    public Response<List<CarSearchResponse>> searchCars(@RequestBody CarSearchRequest req);


}
