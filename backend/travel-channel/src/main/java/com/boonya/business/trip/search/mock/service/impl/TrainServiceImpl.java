package com.boonya.business.trip.search.mock.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchResponse;
import com.boonya.business.trip.search.constant.TrainChannel;
import com.boonya.business.trip.search.mock.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class TrainServiceImpl implements TrainService {

    private static final Random RANDOM = new Random();

    @Override
    @SentinelResource(value = "searchTrains", blockHandler = "handleBlockException", fallback = "handleFallback")
    public List<TrainSearchResponse> searchTrains(TrainSearchRequest req) {
        log.info("搜索火车：{} -> {}, 日期：{}",
                req.getDeparture(), req.getArrival(), req.getDepartureDate());

        List<TrainSearchResponse> results = new ArrayList<>();

        // Mock 不同渠道的火车数据
        for (TrainChannel channel : TrainChannel.values()) {
            List<TrainSearchResponse> channelTrains = searchFromChannel(req, channel);
            results.addAll(channelTrains);
        }

        log.info("搜索到 {} 个火车结果", results.size());
        return results;
    }

    /**
     * 限流处理方法
     */
    public List<TrainSearchResponse> handleBlockException(TrainSearchRequest req, Throwable ex) {
        log.warn("火车查询被限流：{}", ex.getMessage());
        return new ArrayList<>();
    }

    /**
     * 降级处理方法
     */
    public List<TrainSearchResponse> handleFallback(TrainSearchRequest req, Throwable ex) {
        log.error("火车查询服务异常，触发降级：{}", ex.getMessage(), ex);
        return new ArrayList<>();
    }

    private List<TrainSearchResponse> searchFromChannel(TrainSearchRequest req, TrainChannel channel) {
        List<TrainSearchResponse> trains = new ArrayList<>();

        // Mock 4-6 个车次
        int trainCount = RANDOM.nextInt(3) + 4;

        String[] seatTypes = {"二等座", "一等座", "商务座", "硬卧", "软卧"};

        for (int i = 0; i < trainCount; i++) {
            TrainSearchResponse resp = new TrainSearchResponse();
            resp.setTrainNo(generateTrainNo());
            resp.setDeparture(req.getDeparture());
            resp.setArrival(req.getArrival());

            // 设置发车时间
            LocalDateTime deptTime = req.getDepartureDate()
                    .withHour(RANDOM.nextInt(16) + 6)
                    .withMinute(RANDOM.nextInt(60));
            resp.setDepartureTime(deptTime);
            resp.setArrivalTime(deptTime.plusHours(RANDOM.nextInt(12) + 2));

            resp.setSeatType(seatTypes[RANDOM.nextInt(seatTypes.length)]);
            resp.setPrice(generateTrainPrice(resp.getSeatType()));
            resp.setChannel(channel.getDesc());
            resp.setAvailableSeats(RANDOM.nextInt(100) + 20);

            trains.add(resp);
        }

        return trains;
    }

    private String generateTrainNo() {
        char type = new char[]{'G', 'D', 'Z', 'T', 'K'}[RANDOM.nextInt(5)];
        String num = String.format("%04d", RANDOM.nextInt(9000) + 1000);
        return String.valueOf(type) + num;
    }

    private BigDecimal generateTrainPrice(String seatType) {
        BigDecimal basePrice;
        switch (seatType) {
            case "商务座":
                basePrice = new BigDecimal("1500");
                break;
            case "一等座":
                basePrice = new BigDecimal("800");
                break;
            case "软卧":
                basePrice = new BigDecimal("600");
                break;
            case "硬卧":
                basePrice = new BigDecimal("400");
                break;
            default:
                basePrice = new BigDecimal("500");
        }
        // 随机浮动
        int variance = RANDOM.nextInt(200) - 100;
        return basePrice.add(new BigDecimal(variance));
    }
}
