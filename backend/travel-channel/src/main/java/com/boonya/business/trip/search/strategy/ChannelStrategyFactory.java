package com.boonya.business.trip.search.strategy;

import com.boonya.business.trip.common.constant.Scene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelStrategyFactory {

    private final List<ChannelStrategy> strategies;

    // 缓存：key = 订单类型，value = 该类型的所有策略
    private final Map<Scene, List<ChannelStrategy>> strategyCache = new ConcurrentHashMap<>();

    /**
     * 根据订单类型获取所有可用的渠道策略
     */
    public List<ChannelStrategy> getStrategiesByOrderType(Scene orderType) {
        if (orderType == null) {
            throw new IllegalArgumentException("订单类型不能为空");
        }

        return strategyCache.computeIfAbsent(orderType, type ->
                strategies.stream()
                        .filter(strategy -> strategy.getSupportedOrderType() == type)
                        .collect(Collectors.toList())
        );
    }

    /**
     * 根据订单类型和渠道名称获取特定策略
     */
    public ChannelStrategy getStrategy(Scene orderType, String channelName) {
        List<ChannelStrategy> availableStrategies = getStrategiesByOrderType(orderType);

        if (availableStrategies.isEmpty()) {
            throw new IllegalArgumentException("订单类型 " + orderType + " 没有可用的渠道");
        }

        return availableStrategies.stream()
                .filter(strategy -> strategy.getChannelName().equals(channelName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未找到渠道：" + channelName + ", 可用渠道：" +
                                availableStrategies.stream()
                                        .map(ChannelStrategy::getChannelName)
                                        .collect(Collectors.joining(", "))
                ));
    }

    /**
     * 获取默认策略（第一个可用的策略）
     */
    public ChannelStrategy getDefaultStrategy(Scene orderType) {
        List<ChannelStrategy> availableStrategies = getStrategiesByOrderType(orderType);

        if (availableStrategies.isEmpty()) {
            throw new IllegalArgumentException("订单类型 " + orderType + " 没有可用的渠道");
        }

        return availableStrategies.get(0);
    }
}
