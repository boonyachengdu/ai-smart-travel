package com.boonya.business.trip.search.mock.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchResponse;
import com.boonya.business.trip.search.mock.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/channel/mock/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping("/search")
    @Operation(summary = "搜索酒店")
    public Response<List<HotelSearchResponse>> searchHotels(@RequestBody HotelSearchRequest req) {
        List<HotelSearchResponse> results = hotelService.searchHotels(req);
        return Response.ok(results);
    }
}
