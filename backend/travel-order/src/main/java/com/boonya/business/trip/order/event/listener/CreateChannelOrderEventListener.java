package com.boonya.business.trip.order.event.listener;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.Journey;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.entity.Passenger;
import com.boonya.business.trip.common.entity.Ticket;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.order.event.CreateChannelOrderEvent;
import com.boonya.business.trip.order.mapper.OrderMapper;
import com.boonya.business.trip.order.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateChannelOrderEventListener implements ApplicationListener<CreateChannelOrderEvent> {

    private final OrderMapper orderMapper;
    private final TicketMapper ticketMapper;

    private static final Random RANDOM = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApplicationEvent(CreateChannelOrderEvent event) {
        CreateChannelOrderEvent.CreateChannelOrderEventSource source =
                (CreateChannelOrderEvent.CreateChannelOrderEventSource) event.getSource();

        String orderNo = source.getOrderNo();
        log.info("开始生成渠道订单，订单号：{}", orderNo);

        mockCreateChannelOrder(orderNo);
    }

    /**
     * 模拟生成渠道订单
     */
    private void mockCreateChannelOrder(String orderNo) {
        try {
            // 1. 查询订单信息
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Order::getOrderNo, orderNo);
            Order order = orderMapper.selectOne(queryWrapper);

            if (order == null) {
                log.error("订单不存在，订单号：{}", orderNo);
                throw new RuntimeException("订单不存在：" + orderNo);
            }

            // 2. 根据订单类型生成对应的渠道订单
            Scene orderType = order.getOrderType();
            log.info("订单类型：{}, 开始生成对应场景的渠道订单", orderType);

            switch (orderType) {
                case FLIGHT:
                    createFlightChannelOrder(order);
                    break;
                case HOTEL:
                    createHotelChannelOrder(order);
                    break;
                case TRAIN:
                    createTrainChannelOrder(order);
                    break;
                case CAR:
                    createCarChannelOrder(order);
                    break;
                case QA:
                    log.warn("QA 类型订单不需要生成渠道订单");
                    break;
                default:
                    log.error("不支持的订单类型：{}", orderType);
                    throw new RuntimeException("不支持的订单类型：" + orderType);
            }

            log.info("渠道订单生成成功，订单号：{}", orderNo);

        } catch (Exception e) {
            log.error("生成渠道订单失败，订单号：{}", orderNo, e);
        }
    }

    /**
     * 创建航班渠道订单
     */
    private void createFlightChannelOrder(Order order) {
        log.info("开始生成航班渠道订单，订单号：{}", order.getOrderNo());

        List<Journey> journeys = order.getJourney();
        if (journeys == null || journeys.isEmpty()) {
            log.error("航班订单没有行程信息");
            throw new RuntimeException("航班订单缺少行程信息");
        }

        // 为每个行程生成机票
        for (Journey journey : journeys) {
            // 模拟生成航班号
            String flightNo = generateFlightNo(journey.getDeparture(), journey.getArrival());
            journey.setFlightNo(flightNo);

            // 如果没有票信息，生成 mock 票
            if (journey.getTickets() == null || journey.getTickets().isEmpty()) {
                List<Ticket> tickets = createFlightTickets(order, journey);
                journey.setTickets(tickets);
            }

            log.info("航班行程生成成功：{} -> {}, 航班号：{}",
                    journey.getDeparture(), journey.getArrival(), flightNo);
        }

        log.info("航班渠道订单生成完成，订单号：{}", order.getOrderNo());
    }

    /**
     * 创建酒店渠道订单
     */
    private void createHotelChannelOrder(Order order) {
        log.info("开始生成酒店渠道订单，订单号：{}", order.getOrderNo());

        List<Journey> journeys = order.getJourney();
        if (journeys == null || journeys.isEmpty()) {
            log.error("酒店订单没有行程信息");
            throw new RuntimeException("酒店订单缺少行程信息");
        }

        for (Journey journey : journeys) {
            // 模拟生成酒店名称
            String hotelName = generateHotelName(journey.getArrival());
            journey.setHotelName(hotelName);

            log.info("酒店订单生成成功：{}, 入住时间：{}",
                    hotelName, journey.getDepartureTime());
        }

        log.info("酒店渠道订单生成完成，订单号：{}", order.getOrderNo());
    }

    /**
     * 创建火车渠道订单
     */
    private void createTrainChannelOrder(Order order) {
        log.info("开始生成火车渠道订单，订单号：{}", order.getOrderNo());

        List<Journey> journeys = order.getJourney();
        if (journeys == null || journeys.isEmpty()) {
            log.error("火车订单没有行程信息");
            throw new RuntimeException("火车订单缺少行程信息");
        }

        for (Journey journey : journeys) {
            // 模拟生成车次号
            String trainNo = generateTrainNo(journey.getDeparture(), journey.getArrival());
            journey.setTrainNo(trainNo);

            // 生成火车票
            if (journey.getTickets() == null || journey.getTickets().isEmpty()) {
                List<Ticket> tickets = createTrainTickets(order, journey);
                journey.setTickets(tickets);
            }

            log.info("火车行程生成成功：{} -> {}, 车次：{}",
                    journey.getDeparture(), journey.getArrival(), trainNo);
        }

        log.info("火车渠道订单生成完成，订单号：{}", order.getOrderNo());
    }

    /**
     * 创建用车渠道订单
     */
    private void createCarChannelOrder(Order order) {
        log.info("开始生成用车渠道订单，订单号：{}", order.getOrderNo());

        List<Journey> journeys = order.getJourney();
        if (journeys == null || journeys.isEmpty()) {
            log.error("用车订单没有行程信息");
            throw new RuntimeException("用车订单缺少行程信息");
        }

        for (Journey journey : journeys) {
            // 模拟生成车牌号
            String carNo = generateCarNo();
            journey.setCarNo(carNo);

            log.info("用车订单生成成功：{} -> {}, 车牌号：{}",
                    journey.getDeparture(), journey.getArrival(), carNo);
        }

        log.info("用车渠道订单生成完成，订单号：{}", order.getOrderNo());
    }

    /**
     * 创建机票（mock 数据）
     */
    private List<Ticket> createFlightTickets(Order order, Journey journey) {
        List<Ticket> tickets = new ArrayList<>();

        OrderRequirements orderRequirements = JSONObject.parseObject(order.getOrderRequirements(), OrderRequirements.class);
        BigDecimal maxAmount = new BigDecimal(orderRequirements.getBudget());
        // 假设每个乘客一张票
        for (Passenger passenger : orderRequirements.getPassengers()) {
            Ticket ticket = new Ticket();
            ticket.setJourneyId(journey.getId());
            ticket.setCardNo(generateTicketNo("E")); // E 代表电子客票
            ticket.setTravelType(Scene.FLIGHT);
            ticket.setDeparture(journey.getDeparture());
            ticket.setArrival(journey.getArrival());
            ticket.setDepartureTime(journey.getDepartureTime());
            ticket.setArrivalTime(journey.getArrivalTime());
            ticket.setCompanyId(order.getCompanyId());
            ticket.setPassengerId(passenger.getId());
            ticket.setPassengerName(passenger.getName());

            // 随机生成价格
            BigDecimal price = new BigDecimal(String.valueOf(RANDOM.nextInt(maxAmount.intValue())));
            if (price.compareTo(new BigDecimal("200")) < 0) {
                price = new BigDecimal("200");
            }
            ticket.setPrice(price);
            ticket.setTaxFee(new BigDecimal("50"));
            ticket.setInsuranceFee(new BigDecimal("30"));
            // 价格汇总
            BigDecimal amount = price.add(ticket.getTaxFee()).add(ticket.getInsuranceFee());
            ticket.setAmount(amount);

            ticketMapper.insert(ticket);

            tickets.add(ticket);
        }

        return tickets;
    }

    /**
     * 创建火车票（mock 数据）
     */
    private List<Ticket> createTrainTickets(Order order, Journey journey) {
        List<Ticket> tickets = new ArrayList<>();

        OrderRequirements orderRequirements = JSONObject.parseObject(order.getOrderRequirements(), OrderRequirements.class);
        BigDecimal maxAmount = new BigDecimal(orderRequirements.getBudget());
        // 假设每个乘客一张票
        for (Passenger passenger : orderRequirements.getPassengers()) {
            Ticket ticket = new Ticket();
            ticket.setJourneyId(journey.getId());
            ticket.setCardNo(generateTicketNo("T")); // T 代表火车票
            ticket.setTravelType(Scene.TRAIN);
            ticket.setDeparture(journey.getDeparture());
            ticket.setArrival(journey.getArrival());
            ticket.setDepartureTime(journey.getDepartureTime());
            ticket.setArrivalTime(journey.getArrivalTime());
            ticket.setCompanyId(order.getCompanyId());
            ticket.setPassengerId(passenger.getId());
            ticket.setPassengerName(passenger.getName());

            // 随机生成价格
            BigDecimal price = new BigDecimal(String.valueOf(RANDOM.nextInt(maxAmount.intValue())));
            if (price.compareTo(new BigDecimal("200")) < 0) {
                price = new BigDecimal("200");
            }
            ticket.setPrice(price);
            ticket.setTaxFee(new BigDecimal("5"));
            ticket.setInsuranceFee(new BigDecimal("3"));
            // 价格汇总
            BigDecimal amount = price.add(ticket.getTaxFee()).add(ticket.getInsuranceFee());
            ticket.setAmount(amount);

            ticketMapper.insert(ticket);

            tickets.add(ticket);
        }

        return tickets;
    }

    /**
     * 生成航班号（mock）
     */
    private String generateFlightNo(String departure, String arrival) {
        String[] airlines = {"CA", "MU", "CZ", "HU", "ZH", "SC"};
        String airline = airlines[RANDOM.nextInt(airlines.length)];
        String flightNum = String.format("%04d", RANDOM.nextInt(9000) + 1000);
        return airline + flightNum;
    }

    /**
     * 生成车次号（mock）
     */
    private String generateTrainNo(String departure, String arrival) {
        char type = new char[]{'G', 'D', 'Z', 'T', 'K'}[RANDOM.nextInt(5)];
        String num = String.format("%04d", RANDOM.nextInt(9000) + 1000);
        return String.valueOf(type) + num;
    }

    /**
     * 生成酒店名称（mock）
     */
    private String generateHotelName(String city) {
        String[] prefixes = {"锦江", "如家", "汉庭", "全季", "亚朵", "希尔顿", "万豪"};
        String prefix = prefixes[RANDOM.nextInt(prefixes.length)];
        return city + prefix + "酒店";
    }

    /**
     * 生成车牌号（mock）
     */
    private String generateCarNo() {
        String[] cities = {"京", "沪", "粤", "浙", "苏"};
        String city = cities[RANDOM.nextInt(cities.length)];
        String letter = String.valueOf((char) ('A' + RANDOM.nextInt(26)));
        String num = String.format("%05d", RANDOM.nextInt(90000) + 10000);
        return city + letter + num;
    }

    /**
     * 生成票号
     */
    private String generateTicketNo(String prefix) {
        return prefix + System.currentTimeMillis() +
                String.format("%04d", RANDOM.nextInt(9999));
    }
}
