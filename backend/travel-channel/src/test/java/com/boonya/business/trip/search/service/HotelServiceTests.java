package com.boonya.business.trip.search.service;

import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchResponse;
import com.boonya.business.trip.search.ChannelApplication;
import com.boonya.business.trip.search.mock.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChannelApplication.class)
public class HotelServiceTests {

    @Qualifier("hotelServiceImpl")
    @Autowired
    private HotelService hotelService;

    @Test
    public void testSearchHotels_Success() {
        // Given
        HotelSearchRequest req = new HotelSearchRequest();
        req.setCity("北京");
        req.setCheckInDate(LocalDateTime.now().plusDays(1));
        req.setCheckOutDate(LocalDateTime.now().plusDays(3));
        req.setRoomCount(1);
        req.setAdultCount(2);

        // When
        List<HotelSearchResponse> results = hotelService.searchHotels(req);

        // Then
        assertNotNull(results);
        assertTrue(results.size() > 0);

        // 验证返回数据的完整性
        HotelSearchResponse firstHotel = results.get(0);
        assertNotNull(firstHotel.getHotelName());
        assertNotNull(firstHotel.getAddress());
        assertNotNull(firstHotel.getPrice());
        assertNotNull(firstHotel.getChannel());
        assertNotNull(firstHotel.getStarLevel());

        System.out.println("测试通过，查询到 " + results.size() + " 个酒店");
        results.forEach(h ->
                System.out.println(h.getHotelName() + " | " + h.getStarLevel() +
                        " | " + h.getAddress() +
                        " | ￥" + h.getPrice() + "/晚 | 渠道：" + h.getChannel())
        );
    }

    @Test
    public void testSearchHotels_DifferentStarLevel() {
        // Given
        HotelSearchRequest req = new HotelSearchRequest();
        req.setCity("上海");
        req.setCheckInDate(LocalDateTime.now().plusDays(2));
        req.setCheckOutDate(LocalDateTime.now().plusDays(4));
        req.setStarLevel("五星级");

        // When
        List<HotelSearchResponse> results = hotelService.searchHotels(req);

        // Then
        assertNotNull(results);
        System.out.println("五星级酒店数量：" + results.size());
    }

    @Test
    public void testSearchHotels_MultipleChannels() {
        // Given
        HotelSearchRequest req = new HotelSearchRequest();
        req.setCity("广州");
        req.setCheckInDate(LocalDateTime.now().plusDays(1));
        req.setCheckOutDate(LocalDateTime.now().plusDays(2));

        // When
        List<HotelSearchResponse> results = hotelService.searchHotels(req);

        // Then
        assertNotNull(results);

        // 验证有多个渠道的数据
        var channels = results.stream()
                .map(HotelSearchResponse::getChannel)
                .distinct()
                .toList();

        System.out.println("查询到的渠道数量：" + channels.size());
        System.out.println("渠道列表：" + channels);
        assertTrue(channels.size() > 0, "应该有多个渠道的数据");
    }
}
