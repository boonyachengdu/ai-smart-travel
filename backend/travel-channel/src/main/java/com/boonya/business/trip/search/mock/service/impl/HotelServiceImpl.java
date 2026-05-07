package com.boonya.business.trip.search.mock.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchResponse;
import com.boonya.business.trip.search.constant.HotelChannel;
import com.boonya.business.trip.search.mock.service.HotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class HotelServiceImpl implements HotelService {

    private static final Random RANDOM = new Random();

    @Override
    @SentinelResource(value = "searchHotels", blockHandler = "handleBlockException", fallback = "handleFallback")
    public List<HotelSearchResponse> searchHotels(HotelSearchRequest req) {
        log.info("搜索酒店：城市：{}, 入住：{}, 离店：{}",
                req.getCity(), req.getCheckInDate(), req.getCheckOutDate());

        List<HotelSearchResponse> results = new ArrayList<>();

        // Mock 不同渠道的酒店数据
        for (HotelChannel channel : HotelChannel.values()) {
            List<HotelSearchResponse> channelHotels = searchFromChannel(req, channel);
            results.addAll(channelHotels);
        }

        log.info("搜索到 {} 个酒店结果", results.size());
        return results;
    }

    /**
     * 限流处理方法
     */
    public List<HotelSearchResponse> handleBlockException(HotelSearchRequest req, Throwable ex) {
        log.warn("酒店查询被限流：{}", ex.getMessage());
        return new ArrayList<>();
    }

    /**
     * 降级处理方法
     */
    public List<HotelSearchResponse> handleFallback(HotelSearchRequest req, Throwable ex) {
        log.error("酒店查询服务异常，触发降级：{}", ex.getMessage(), ex);
        return new ArrayList<>();
    }

    private List<HotelSearchResponse> searchFromChannel(HotelSearchRequest req, HotelChannel channel) {
        List<HotelSearchResponse> hotels = new ArrayList<>();

        // Mock 2-4 个酒店
        int hotelCount = RANDOM.nextInt(3) + 2;

        String[] starLevels = {"三星级", "四星级", "五星级", "豪华型"};
        String[] amenities = {"WiFi,早餐,停车场", "WiFi,健身房,游泳池", "WiFi,餐厅,会议室", "WiFi,SPA,机场接送"};

        for (int i = 0; i < hotelCount; i++) {
            HotelSearchResponse resp = new HotelSearchResponse();
            resp.setHotelName(generateHotelName(req.getCity()));
            resp.setAddress(req.getCity() + "市" + RANDOM.nextInt(100) + "号");
            resp.setStarLevel(starLevels[RANDOM.nextInt(starLevels.length)]);
            resp.setCheckInDate(req.getCheckInDate());
            resp.setCheckOutDate(req.getCheckOutDate());
            resp.setPrice(generateHotelPrice(resp.getStarLevel()));
            resp.setChannel(channel.getDesc());
            resp.setAvailableRooms(RANDOM.nextInt(20) + 5);
            resp.setAmenities(amenities[RANDOM.nextInt(amenities.length)]);

            hotels.add(resp);
        }

        return hotels;
    }

    private String generateHotelName(String city) {
        String[] prefixes = {"锦江", "如家", "汉庭", "全季", "亚朵", "希尔顿", "万豪", "洲际"};
        String prefix = prefixes[RANDOM.nextInt(prefixes.length)];
        return city + prefix + "酒店";
    }

    private BigDecimal generateHotelPrice(String starLevel) {
        BigDecimal basePrice;
        switch (starLevel) {
            case "五星级":
            case "豪华型":
                basePrice = new BigDecimal("800");
                break;
            case "四星级":
                basePrice = new BigDecimal("500");
                break;
            default:
                basePrice = new BigDecimal("300");
        }
        // 随机浮动
        int variance = RANDOM.nextInt(200) - 100;
        return basePrice.add(new BigDecimal(variance));
    }
}
