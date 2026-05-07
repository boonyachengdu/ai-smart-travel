package com.boonya.business.trip.search.strategy;

import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Order;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractChannelStrategy implements ChannelStrategy {

    @Override
    public Order placeOrder(Order order) {
        log.info("[{}] 开始下单，订单号：{}", getChannelName(), order.getOrderNo());

        try {
            // 1. 验证订单类型是否匹配
            if (!getSupportedOrderType().equals(order.getOrderType())) {
                throw new IllegalArgumentException(
                        String.format("订单类型不匹配，期望：%s, 实际：%s",
                                getSupportedOrderType(), order.getOrderType()));
            }

            // 2. 执行具体的下单逻辑（由子类实现）
            return doPlaceOrder(order);

        } catch (Exception e) {
            log.error("[{}] 下单失败，订单号：{}, 错误：{}",
                    getChannelName(), order.getOrderNo(), e.getMessage(), e);
            throw new RuntimeException("渠道下单失败：" + e.getMessage(), e);
        }
    }

    /**
     * 执行具体的下单逻辑
     */
    protected abstract Order doPlaceOrder(Order order);
}
