package com.boonya.business.trip.search.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.models.channel.CarSearchRequest;
import com.boonya.business.trip.common.models.channel.FlightSearchRequest;
import com.boonya.business.trip.common.models.channel.HotelSearchRequest;
import com.boonya.business.trip.common.models.channel.TrainSearchRequest;
import com.boonya.business.trip.search.strategy.ChannelStrategy;
import com.boonya.business.trip.search.strategy.ChannelStrategyFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelStrategyFactory strategyFactory;

    @GetMapping("/strategies")
    @Operation(summary = "获取所有可用的渠道策略")
    public Response<List<Map<String, String>>> getAllStrategies() {
        List<Map<String, String>> strategies = strategyFactory.getStrategiesByOrderType(null)
                .stream()
                .map(strategy -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("channelName", strategy.getChannelName());
                    info.put("orderType", strategy.getSupportedOrderType().name());
                    return info;
                })
                .collect(Collectors.toList());
        return Response.ok(strategies);
    }

    @PostMapping("/search/flight")
    @Operation(summary = "搜索航班信息")
    public Response<Object> searchFlights(
            @RequestBody FlightSearchRequest request,
            @RequestParam(required = false) String channelName) {
        try {
            log.info("搜索航班，出发地：{}, 目的地：{}, 日期：{}, 渠道：{}",
                    request.getDeparture(), request.getArrival(),
                    request.getDepartureDate(), channelName);

            List<Object> results = searchProducts(Scene.FLIGHT, request, channelName);
            return Response.ok(results);
        } catch (Exception e) {
            log.error("搜索航班失败", e);
            return Response.error("搜索失败：" + e.getMessage());
        }
    }

    @PostMapping("/search/hotel")
    @Operation(summary = "搜索酒店信息")
    public Response<Object> searchHotels(
            @RequestBody HotelSearchRequest request,
            @RequestParam(required = false) String channelName) {
        try {
            log.info("搜索酒店，城市：{}, 入住日期：{}, 渠道：{}",
                    request.getCity(), request.getCheckInDate(), channelName);

            List<Object> results = searchProducts(Scene.HOTEL, request, channelName);
            return Response.ok(results);
        } catch (Exception e) {
            log.error("搜索酒店失败", e);
            return Response.error("搜索失败：" + e.getMessage());
        }
    }

    @PostMapping("/search/train")
    @Operation(summary = "搜索火车票信息")
    public Response<Object> searchTrains(
            @RequestBody TrainSearchRequest request,
            @RequestParam(required = false) String channelName) {
        try {
            log.info("搜索火车，出发地：{}, 目的地：{}, 日期：{}, 渠道：{}",
                    request.getDeparture(), request.getArrival(),
                    request.getDepartureDate(), channelName);

            List<Object> results = searchProducts(Scene.TRAIN, request, channelName);
            return Response.ok(results);
        } catch (Exception e) {
            log.error("搜索火车失败", e);
            return Response.error("搜索失败：" + e.getMessage());
        }
    }

    @PostMapping("/search/car")
    @Operation(summary = "搜索车辆信息")
    public Response<Object> searchCars(
            @RequestBody CarSearchRequest request,
            @RequestParam(required = false) String channelName) {
        try {
            log.info("搜索用车，城市：{}, 用车时间：{}, 目的地：{}, 渠道：{}",
                    request.getCity(), request.getUseTime(),
                    request.getDestination(), channelName);

            List<Object> results = searchProducts(Scene.CAR, request, channelName);
            return Response.ok(results);
        } catch (Exception e) {
            log.error("搜索用车失败", e);
            return Response.error("搜索失败：" + e.getMessage());
        }
    }


    @GetMapping("/products/{orderType}")
    @Operation(summary = "根据订单类型查询渠道商品")
    public Response<List<Object>> getProducts(
            @PathVariable("orderType") @Parameter(description = "订单类型 (FLIGHT/HOTEL/TRAIN)") String orderType,
            @RequestParam Map<String, Object> params,
            @RequestParam(required = false) String channelName) {
        try {
            Scene scene = Scene.valueOf(orderType.toUpperCase());
            log.info("查询商品，订单类型：{}, 渠道：{}, 参数：{}", orderType, channelName, params);

            List<Object> results = searchProducts(scene, params, channelName);
            return Response.ok(results);
        } catch (Exception e) {
            log.error("查询商品失败", e);
            return Response.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 通用的商品搜索方法
     */
    private List<Object> searchProducts(Scene orderType, Object searchRequest, String channelName) {
        ChannelStrategy strategy;
        if (channelName != null && !channelName.trim().isEmpty()) {
            strategy = strategyFactory.getStrategy(orderType, channelName);
        } else {
            strategy = strategyFactory.getDefaultStrategy(orderType);
        }

        log.info("使用渠道策略：[{}], 订单类型：{}", strategy.getChannelName(), orderType);

        Map<String, Object> params = convertToMap(searchRequest);
        Object result = strategy.searchProducts(params);

        if (result instanceof List) {
            return (List<Object>) result;
        } else {
            return List.of(result);
        }
    }

    /**
     * 将对象转换为 Map
     */
    private Map<String, Object> convertToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        if (obj == null) {
            return map;
        }

        try {
            Class<?> clazz = obj.getClass();
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    map.put(field.getName(), value);
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("转换对象为 Map 失败", e);
        }
        return map;
    }
}
