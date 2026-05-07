package com.boonya.business.trip.search.strategy.impl;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.search.constant.TrainChannel;
import com.boonya.business.trip.search.strategy.AbstractChannelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TrainGaotieguanjiaChannelStrategy extends AbstractChannelStrategy {

    @Override
    protected Order doPlaceOrder(Order order) {
        log.info("[高铁管家] 火车下单，订单号：{}", order.getOrderNo());

        // TODO: 调用 高铁管家 API 下单

        log.info("[高铁管家] 火车下单成功，订单号：{}", order.getOrderNo());
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
        return Scene.TRAIN;
    }

    @Override
    public String getChannelName() {
        return TrainChannel.GAO_TIE_GUAN_JIA.getDesc();
    }
}
