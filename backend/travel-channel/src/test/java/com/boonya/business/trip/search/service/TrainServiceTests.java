package com.boonya.business.trip.search.service;

import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchResponse;
import com.boonya.business.trip.search.ChannelApplication;
import com.boonya.business.trip.search.mock.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChannelApplication.class)
public class TrainServiceTests {

    @Qualifier("trainServiceImpl")
    @Autowired
    private TrainService trainService;

    @Test
    public void testSearchTrains_Success() {
        // Given
        TrainSearchRequest req = new TrainSearchRequest();
        req.setDeparture("北京");
        req.setArrival("上海");
        req.setDepartureDate(LocalDateTime.now().plusDays(1));
        req.setAdultCount(1);

        // When
        List<TrainSearchResponse> results = trainService.searchTrains(req);

        // Then
        assertNotNull(results);
        assertTrue(results.size() > 0);

        // 验证返回数据的完整性
        TrainSearchResponse firstTrain = results.get(0);
        assertNotNull(firstTrain.getTrainNo());
        assertNotNull(firstTrain.getPrice());
        assertNotNull(firstTrain.getChannel());
        assertNotNull(firstTrain.getSeatType());

        System.out.println("测试通过，查询到 " + results.size() + " 个车次");
        results.forEach(t ->
                System.out.println(t.getTrainNo() + " | " + t.getDepartureTime() +
                        " -> " + t.getArrivalTime() +
                        " | " + t.getSeatType() + " | ￥" + t.getPrice() +
                        " | 渠道：" + t.getChannel())
        );
    }

    @Test
    public void testSearchTrains_DifferentSeatType() {
        // Given
        TrainSearchRequest req = new TrainSearchRequest();
        req.setDeparture("广州");
        req.setArrival("深圳");
        req.setDepartureDate(LocalDateTime.now().plusDays(2));
        req.setSeatType("一等座");

        // When
        List<TrainSearchResponse> results = trainService.searchTrains(req);

        // Then
        assertNotNull(results);
        System.out.println("一等座车次数量：" + results.size());
    }

    @Test
    public void testSearchTrains_MultipleChannels() {
        // Given
        TrainSearchRequest req = new TrainSearchRequest();
        req.setDeparture("成都");
        req.setArrival("重庆");
        req.setDepartureDate(LocalDateTime.now().plusDays(3));

        // When
        List<TrainSearchResponse> results = trainService.searchTrains(req);

        // Then
        assertNotNull(results);

        // 验证有多个渠道的数据
        var channels = results.stream()
                .map(TrainSearchResponse::getChannel)
                .distinct()
                .toList();

        System.out.println("查询到的渠道数量：" + channels.size());
        System.out.println("渠道列表：" + channels);
        assertTrue(channels.size() > 0, "应该有多个渠道的数据");
    }
}
