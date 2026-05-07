package com.boonya.business.trip.search.strategy.impl;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.channel.CarSearchResponse;
import com.boonya.business.trip.search.strategy.AbstractChannelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DidiCarChannelStrategy extends AbstractChannelStrategy {

    //private final CarService carService;

    @Override
    public List searchProducts(Map params) {
        log.info("【滴滴出行】搜索用车，参数：{}", params);

        com.boonya.business.trip.common.models.channel.CarSearchRequest request =
                new com.boonya.business.trip.common.models.channel.CarSearchRequest();
        request.setCity((String) params.get("city"));
        request.setUseTime((java.time.LocalDateTime) params.get("useTime"));
        request.setDestination((String) params.get("destination"));
        request.setCarType((String) params.get("carType"));

        // TODO: 调用滴滴出行 API 搜索用车
        List<CarSearchResponse> results = new ArrayList<>();//carService.searchCars(request);

        log.info("【滴滴出行】返回 {} 个用车选项", results.size());
        return results;
    }

    @Override
    public Object queryOrderDetail(String channelOrderNo) {
        return null;
    }

    @Override
    public Response<?> cancelOrder(Order order) {
        return null;
    }

    @Override
    public Response<?> refundOrder(Order order) {
        return null;
    }

    @Override
    public Scene getSupportedOrderType() {
        return Scene.CAR;
    }

    @Override
    public String getChannelName() {
        return "滴滴出行";
    }

    @Override
    protected Order doPlaceOrder(Order order) {
        return null;
    }
}
