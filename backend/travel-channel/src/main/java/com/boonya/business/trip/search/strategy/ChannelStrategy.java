package com.boonya.business.trip.search.strategy;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Order;

import java.util.List;
import java.util.Map;

/**
 * 渠道策略接口
 */
public interface ChannelStrategy<T> {

    /**
     * 查询渠道商品列表（如航班列表、酒店、火车车次列表）
     * @param params 查询参数（出发地、目的地、日期等）
     * @return 查询结果
     */
    List<T> searchProducts(Map<String, Object> params);

    /**
     * 查询渠道订单详情
     * @param channelOrderNo 渠道订单号
     * @return 订单详情
     */
    T queryOrderDetail(String channelOrderNo);

    /**
     * 向渠道下单
     *
     * @param order 订单信息
     * @return 下单后的订单信息
     */
    Order placeOrder(Order order);

    /**
     * 渠道退订
     *
     * @param order 订单信息
     * @return 退订结果
     */
    Response<?> cancelOrder(Order order);

    /**
     * 渠道退款
     *
     * @param order 订单信息
     * @return 退订结果
     */
    Response<?> refundOrder(Order order);

    /**
     * 获取支持的订单类型
     *
     * @return 订单类型枚举
     */
    Scene getSupportedOrderType();

    /**
     * 获取渠道名称
     *
     * @return 渠道名称
     */
    String getChannelName();
}
