package com.boonya.business.trip.search.strategy.impl;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.search.strategy.AbstractChannelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FeizhuFlightChannelStrategy extends AbstractChannelStrategy {

    @Override
    protected Order doPlaceOrder(Order order) {
        log.info("[飞猪] 航班下单，订单号：{}", order.getOrderNo());

        // TODO: 调用飞猪 API 下单

        log.info("[飞猪] 航班下单成功，订单号：{}", order.getOrderNo());
        return order;
    }

    @Override
    public List searchProducts(Map params) {
        return null;
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
        return Scene.FLIGHT;
    }

    @Override
    public String getChannelName() {
        return "飞猪";
    }
}
