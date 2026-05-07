package com.boonya.business.trip.search.mock.service;

import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchResponse;

import java.util.List;

public interface FlightService {

    List<FlightSearchResponse> searchFlights(FlightSearchRequest req);
}
