package com.boonya.business.trip.search.service;

import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchResponse;
import com.boonya.business.trip.search.ChannelApplication;
import com.boonya.business.trip.search.mock.service.FlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChannelApplication.class)
public class FlightServiceTests {

    @Qualifier("flightServiceImpl")
    @Autowired
    private FlightService flightService;

    @Test
    public void testSearchFlights_Success() {
        // Given
        FlightSearchRequest req = new FlightSearchRequest();
        req.setDeparture("北京");
        req.setArrival("上海");
        req.setDepartureDate(LocalDateTime.now().plusDays(1));
        req.setAdultCount(1);

        // When
        List<FlightSearchResponse> results = flightService.searchFlights(req);

        // Then
        assertNotNull(results);
        assertTrue(results.size() > 0);

        // 验证返回数据的完整性
        FlightSearchResponse firstFlight = results.get(0);
        assertNotNull(firstFlight.getFlightNo());
        assertNotNull(firstFlight.getAirline());
        assertNotNull(firstFlight.getPrice());
        assertNotNull(firstFlight.getChannel());

        System.out.println("测试通过，查询到 " + results.size() + " 个航班");
        results.forEach(f ->
                System.out.println(f.getFlightNo() + " | " + f.getAirline() +
                        " | " + f.getDepartureTime() + " -> " + f.getArrivalTime() +
                        " | ￥" + f.getPrice() + " | 渠道：" + f.getChannel())
        );
    }

    @Test
    public void testSearchFlights_DifferentCabinClass() {
        // Given
        FlightSearchRequest req = new FlightSearchRequest();
        req.setDeparture("广州");
        req.setArrival("深圳");
        req.setDepartureDate(LocalDateTime.now().plusDays(2));
        req.setCabinClass("公务舱");

        // When
        List<FlightSearchResponse> results = flightService.searchFlights(req);

        // Then
        assertNotNull(results);
        System.out.println("公务舱航班数量：" + results.size());
    }

    @Test
    public void testSearchFlights_MultipleChannels() {
        // Given
        FlightSearchRequest req = new FlightSearchRequest();
        req.setDeparture("成都");
        req.setArrival("杭州");
        req.setDepartureDate(LocalDateTime.now().plusDays(3));

        // When
        List<FlightSearchResponse> results = flightService.searchFlights(req);

        // Then
        assertNotNull(results);

        // 验证有多个渠道的数据
        var channels = results.stream()
                .map(FlightSearchResponse::getChannel)
                .distinct()
                .toList();

        System.out.println("查询到的渠道数量：" + channels.size());
        System.out.println("渠道列表：" + channels);
        assertTrue(channels.size() > 0, "应该有多个渠道的数据");
    }
}
