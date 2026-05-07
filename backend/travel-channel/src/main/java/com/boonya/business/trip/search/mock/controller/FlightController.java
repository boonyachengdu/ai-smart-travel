package com.boonya.business.trip.search.mock.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchResponse;
import com.boonya.business.trip.search.mock.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/channel/mock/flight")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping("/search")
    @Operation(summary = "搜索航班")
    public Response<List<FlightSearchResponse>> searchFlights(@RequestBody FlightSearchRequest req) {
        List<FlightSearchResponse> results = flightService.searchFlights(req);
        return Response.ok(results);
    }
}
