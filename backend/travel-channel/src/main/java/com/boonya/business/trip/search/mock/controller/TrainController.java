package com.boonya.business.trip.search.mock.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchResponse;
import com.boonya.business.trip.search.mock.service.TrainService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/channel/mock/train")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @PostMapping("/search")
    @Operation(summary = "搜索火车")
    public Response<List<TrainSearchResponse>> searchTrains(@RequestBody TrainSearchRequest req) {
        List<TrainSearchResponse> results = trainService.searchTrains(req);
        return Response.ok(results);
    }

}