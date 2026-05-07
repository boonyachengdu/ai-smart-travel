package com.boonya.business.trip.search.mock.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchResponse;
import com.boonya.business.trip.search.constant.FlightChannel;
import com.boonya.business.trip.search.mock.service.FlightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
public class FlightServiceImpl implements FlightService {

    private static final Random RANDOM = new Random();

    @Override
    @SentinelResource(value = "searchFlights", blockHandler = "handleBlockException", fallback = "handleFallback")
    public List<FlightSearchResponse> searchFlights(FlightSearchRequest req) {
        log.info("搜索航班：{} -> {}, 日期：{}",
                req.getDeparture(), req.getArrival(), req.getDepartureDate());

        List<FlightSearchResponse> results = new ArrayList<>();

        // Mock 不同渠道的航班数据
        for (FlightChannel channel : FlightChannel.values()) {
            List<FlightSearchResponse> channelFlights = searchFromChannel(req, channel);
            results.addAll(channelFlights);
        }

        log.info("搜索到 {} 个航班结果", results.size());
        return results;
    }

    /**
     * 限流处理方法
     */
    public List<FlightSearchResponse> handleBlockException(FlightSearchRequest req, Throwable ex) {
        log.warn("航班查询被限流：{}", ex.getMessage());
        return new ArrayList<>();
    }

    /**
     * 降级处理方法
     */
    public List<FlightSearchResponse> handleFallback(FlightSearchRequest req, Throwable ex) {
        log.error("航班查询服务异常，触发降级：{}", ex.getMessage(), ex);
        return new ArrayList<>();
    }

    private List<FlightSearchResponse> searchFromChannel(FlightSearchRequest req, FlightChannel channel) {
        List<FlightSearchResponse> flights = new ArrayList<>();

        // Mock 3-5 个航班
        int flightCount = RANDOM.nextInt(3) + 3;

        String[] airlines = {"中国国际航空", "东方航空", "南方航空", "海南航空", "深圳航空", "山东航空"};
        String[] cabinClasses = {"经济舱", "公务舱", "头等舱"};

        for (int i = 0; i < flightCount; i++) {
            FlightSearchResponse resp = new FlightSearchResponse();
            resp.setFlightNo(generateFlightNo());
            resp.setAirline(airlines[RANDOM.nextInt(airlines.length)]);
            resp.setDeparture(req.getDeparture());
            resp.setArrival(req.getArrival());

            // 设置起降时间
            LocalDateTime deptTime = req.getDepartureDate()
                    .withHour(RANDOM.nextInt(18) + 6)
                    .withMinute(RANDOM.nextInt(60));
            resp.setDepartureTime(deptTime);
            resp.setArrivalTime(deptTime.plusHours(RANDOM.nextInt(4) + 2));

            resp.setCabinClass(cabinClasses[RANDOM.nextInt(cabinClasses.length)]);
            resp.setPrice(generateFlightPrice(req, resp.getCabinClass()));
            resp.setChannel(channel.getDesc());
            resp.setAvailableSeats(RANDOM.nextInt(50) + 10);

            flights.add(resp);
        }

        return flights;
    }

    private String generateFlightNo() {
        String[] airlines = {"CA", "MU", "CZ", "HU", "ZH", "SC"};
        String airline = airlines[RANDOM.nextInt(airlines.length)];
        String num = String.format("%04d", RANDOM.nextInt(9000) + 1000);
        return airline + num;
    }

    private BigDecimal generateFlightPrice(FlightSearchRequest req, String cabinClass) {
        BigDecimal basePrice;
        switch (cabinClass) {
            case "头等舱":
                basePrice = new BigDecimal("5000");
                break;
            case "公务舱":
                basePrice = new BigDecimal("3000");
                break;
            default:
                basePrice = new BigDecimal("1500");
        }

        // 价格过滤时进行处理
        if (Objects.nonNull(req.getMaxBudget()) || Objects.nonNull(req.getMinBudget())) {
            BigDecimal minPrice = basePrice.multiply(new BigDecimal("0.8"));
            BigDecimal maxPrice = basePrice.multiply(new BigDecimal("1.2"));

            if (req.getMaxBudget() != null && req.getMinBudget() != null) {
                // 同时有最高和最低预算限制
                BigDecimal budgetMin = req.getMinBudget();
                BigDecimal budgetMax = req.getMaxBudget();

                // 确保预算范围有效
                if (budgetMin.compareTo(budgetMax) <= 0) {
                    // 在预算范围内随机生成价格
                    return generatePriceInRange(budgetMin, budgetMax);
                } else {
                    // 预算范围无效，返回基础价格
                    return basePrice;
                }
            } else if (req.getMaxBudget() != null) {
                // 只有最高预算限制
                BigDecimal budgetMax = req.getMaxBudget();

                // 如果最高预算低于最低价，返回接近最高预算的价格
                if (budgetMax.compareTo(minPrice) < 0) {
                    return generatePriceInRange(budgetMax.multiply(new BigDecimal("0.9")), budgetMax);
                } else {
                    // 在最低价和最高预算之间随机
                    return generatePriceInRange(minPrice, budgetMax.min(maxPrice));
                }
            } else if (req.getMinBudget() != null) {
                // 只有最低预算限制
                BigDecimal budgetMin = req.getMinBudget();

                // 如果最低预算高于最高价，返回接近最低预算的价格
                if (budgetMin.compareTo(maxPrice) > 0) {
                    return generatePriceInRange(budgetMin, budgetMin.multiply(new BigDecimal("1.1")));
                } else {
                    // 在最低预算和最高价之间随机
                    return generatePriceInRange(budgetMin.max(minPrice), maxPrice);
                }
            }
        }

        // 无预算限制，在基础价格上下浮动
        int variance = RANDOM.nextInt(1000) - 500;
        return basePrice.add(new BigDecimal(variance));
    }

    private BigDecimal generatePriceInRange(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) >= 0) {
            return min;
        }

        // 计算差值（以分为单位）
        long diff = max.subtract(min).multiply(new BigDecimal("100")).longValue();

        if (diff <= 0) {
            return min;
        }

        // 随机生成分值
        long randomCents = RANDOM.nextInt((int) Math.min(diff, Integer.MAX_VALUE - 1)) + 1;

        // 转换为元并加上最小值
        return min.add(new BigDecimal(randomCents).divide(new BigDecimal("100")));
    }

}
