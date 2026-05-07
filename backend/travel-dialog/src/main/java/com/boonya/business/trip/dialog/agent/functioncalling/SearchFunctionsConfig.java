package com.boonya.business.trip.dialog.agent.functioncalling;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.models.channel.CarSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.feign.clients.ChannelFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class SearchFunctionsConfig {

    private final ChannelFeignClient channelFeignClient;

    @Bean
    @Description("搜索航班信息，参数包括出发城市、到达城市、出发日期")
    public Function<FlightSearchRequest, Response<?>> searchFlights() {
        return channelFeignClient::searchFlights;
    }

    @Bean
    @Description("搜索酒店信息，参数包括城市、入住日期、退房日期")
    public Function<HotelSearchRequest, Response<?>> searchHotels() {
        return channelFeignClient::searchHotels;
    }

    @Bean
    @Description("搜索火车信息，参数包括出发城市、到达城市、出发日期")
    public Function<TrainSearchRequest, Response<?>> searchTrains() {
        return channelFeignClient::searchTrains;
    }

    @Bean
    @Description("搜索用车信息，参数包括车型、出发地点、到达地点、出发时间")
    public Function<CarSearchRequest, Response<?>> searchCars() {
        return channelFeignClient::searchCars;
    }
}
