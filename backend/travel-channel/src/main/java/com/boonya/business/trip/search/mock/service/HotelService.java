package com.boonya.business.trip.search.mock.service;

import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchResponse;

import java.util.List;

public interface HotelService {

    List<HotelSearchResponse> searchHotels(HotelSearchRequest req);
}
