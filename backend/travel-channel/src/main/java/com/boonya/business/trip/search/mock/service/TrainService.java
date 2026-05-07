package com.boonya.business.trip.search.mock.service;

import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchResponse;

import java.util.List;

public interface TrainService {

    List<TrainSearchResponse> searchTrains(TrainSearchRequest req);
}
