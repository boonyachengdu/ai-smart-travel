package com.boonya.business.trip.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.constant.OrderStatus;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.constant.TripType;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Journey;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.feign.clients.ApprovalFeignClient;
import com.boonya.business.trip.feign.clients.CompanyFeignClient;
import com.boonya.business.trip.order.event.PayEvent;
import com.boonya.business.trip.order.mapper.JourneyMapper;
import com.boonya.business.trip.order.mapper.OrderMapper;
import com.boonya.business.trip.order.service.ApprovalFlowService;
import com.boonya.business.trip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    private final JourneyMapper journeyMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApprovalFlowService approvalFlowService;
    private final CompanyFeignClient companyFeignClient;
    private final ApprovalFeignClient approvalFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderGenerationResponse createOrder(OrderRequirements requirements) {
        log.info("开始生成订单，订单类型：{}", requirements.getOrderType());

        try {
            // 1. 获取当前用户上下文
            UserHolder userContext = UserHolder.get();
            if (userContext == null) {
                log.info("用户上下文为空，无法下单： 下单参数- {}", JSONObject.toJSONString(requirements));
                return new OrderGenerationResponse(null, "用户上下文为空", null, null, null, null, null, null);
            }

            // 2. 创建订单实体
            Order order = new Order();

            // 基本信息
            order.setOrderNo(generateOrderNo(requirements.getOrderType()));
            order.setCompanyId(userContext.getCompanyId());
            order.setUserId(userContext.getUserId());
            order.setUsername(userContext.getUsername());

            // 订单类型转换
            Scene orderType = convertToScene(requirements.getOrderType());
            order.setOrderType(orderType);

            // 金额
            if (requirements.getBudget() != null) {
                order.setAmount(new BigDecimal(requirements.getBudget().toString()));
            }

            // 状态
            order.setStatus(OrderStatus.DRAFT); // 草稿
            order.setOrderRequirements(JSONObject.toJSONString(requirements));
            order.setCreateTime(LocalDateTime.now());

            // 3. 保存订单
            boolean saved = save(order);
            if (saved) {
                // 4. 构建行程列表
                List<Journey> journeys = buildJourneys(order, requirements);
                for (Journey journey : journeys) {
                    journey.setOrderId(order.getId());
                    journeyMapper.insert(journey);
                }
                order.setJourney(journeys);
                order.setUpdateTime(LocalDateTime.now());
                updateById(order);
            } else {
                throw new RuntimeException("订单保存失败");
            }

            log.info("订单生成成功，订单号：{}, 订单 ID: {}", order.getOrderNo(), order.getId());

            // 5. 构建返回结果
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setStatus("complete");
            response.setMessage("订单生成成功");

            // 将生成的订单信息填充到 response 的 order 字段
            requirements.setOrderNo(order.getOrderNo());
            response.setOrder(requirements);

            Response<Company> companyResponse = companyFeignClient.getById(order.getCompanyId());
            Company company = companyResponse.getData();

            // 调用审批流引擎
            if (companyResponse.isSuccess() && companyResponse.getData() != null) {
                boolean needAudit = approvalFlowService.needApproval(order, company);

                if (needAudit) {
                    Approval approval = approvalFlowService.createApprovalFlow(company, order, userContext);
                    order.setAuditStatus(approval.getStatus());
                    order.setApprovalId(approval.getId());
                    log.info("创建审批流程，审批 ID: {}", approval.getId());
                } else {
                    order.setAuditStatus(AuditStatus.APPROVED);
                }

                this.updateById(order);
            }

            return response;

        } catch (Exception e) {
            log.error("订单生成失败", e);
            OrderGenerationResponse errorResponse = new OrderGenerationResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("订单生成失败：" + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    public Order getByApprovalId(Long approvalId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getDeleted, 0);
        wrapper.eq(Order::getApprovalId, approvalId);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Boolean approved(Order temp, String remark) {
        Approval approval = new Approval();
        approval.setId(temp.getApprovalId());
        approval.setStatus(AuditStatus.APPROVED);
        approval.setRemark(remark);

        Response<?> response = approvalFeignClient.approveCallback(temp.getApprovalId(), approval);
        if (response.isSuccess()) {
            approvedCallback(temp, remark);
            return true;
        }

        log.error("审批回调失败：{}", response.getMessage());
        return false;
    }

    @Override
    public Boolean approvedCallback(Order temp, String remark) {
        Order order = new Order();
        order.setId(temp.getId());
        order.setAuditStatus(AuditStatus.APPROVED);
        order.setApprovalId(temp.getApprovalId());
        order.setUpdateTime(LocalDateTime.now());
        order.setRemark(remark);
        baseMapper.updateById(order);
        log.info("订单审批状态已更新：orderNo={}, approvalId={}, status=APPROVED",
                order.getOrderNo(), temp.getApprovalId());

        applicationEventPublisher.publishEvent(new PayEvent(order));

        return true;
    }

    @Override
    public Boolean rejected(Order temp, String reason) {
        String message = "审批拒绝：" + reason;
        Approval approval = new Approval();
        approval.setId(temp.getApprovalId());
        approval.setStatus(AuditStatus.REJECTED);
        approval.setRemark(message);

        Response<?> response = approvalFeignClient.approveCallback(temp.getApprovalId(), approval);
        if (response.isSuccess()) {
            rejectedCallback(temp, message);
            return true;
        }

        log.error("审批回调失败：{}", response.getMessage());
        return false;
    }

    @Override
    public Boolean rejectedCallback(Order temp, String reason) {
        Order order = new Order();
        order.setId(temp.getId());
        order.setAuditStatus(AuditStatus.REJECTED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setRemark(reason);
        order.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(order);
        log.info("订单已作废：orderNo={}, approvalId={}, status=CANCELLED, reason={}",
                order.getOrderNo(), order.getApprovalId(), reason);
        return true;
    }

    @Override
    public Long totalOrders() {
        UserHolder userHolder = UserHolder.get();
        if (userHolder.isSuperAdmin()) {
            return baseMapper.countAllOrders();
        } else {
            return baseMapper.countByCompanyId(userHolder.getCompanyId());
        }
    }

    @Override
    public Long monthOrders() {
        UserHolder userHolder = UserHolder.get();
        // 获取本月 1 号 00:00:00
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        if (userHolder.isSuperAdmin()) {
            return baseMapper.countByMonth(null, startOfMonth);
        } else {
            return baseMapper.countByMonth(userHolder.getCompanyId(), startOfMonth);
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo(String orderType) {
        // 格式：OT + 订单类型首字母 + 时间戳 + 随机数
        String typePrefix = "";
        if (StringUtils.isNotBlank(orderType)) {
            typePrefix = orderType.substring(0, 1).toUpperCase();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();

        return "OT" + typePrefix + timestamp + random;
    }

    /**
     * 构建行程列表
     */
    private List<Journey> buildJourneys(Order order, OrderRequirements requirements) {
        UserHolder userHolder = UserHolder.get();
        List<Journey> journeys = new ArrayList<>();

        // 去程
        if (StringUtils.isNotBlank(requirements.getDepartureCity()) &&
                StringUtils.isNotBlank(requirements.getArrivalCity())) {

            Journey outboundJourney = new Journey();
            outboundJourney.setCompanyId(userHolder.getCompanyId());
            outboundJourney.setDeparture(requirements.getDepartureCity());
            outboundJourney.setArrival(requirements.getArrivalCity());

            if (StringUtils.isNotBlank(requirements.getDepartureDate())) {
                try {
                    LocalDate departureDate = LocalDate.parse(requirements.getDepartureDate(),
                            DateTimeFormatter.ISO_LOCAL_DATE);
                    outboundJourney.setDepartureTime(departureDate.atStartOfDay());
                } catch (Exception e) {
                    log.warn("出发时间解析失败：{}", requirements.getDepartureDate());
                }
            }

            if (StringUtils.isNotBlank(requirements.getArrivalDate())) {
                try {
                    LocalDate arrivalDate = LocalDate.parse(requirements.getArrivalDate(),
                            DateTimeFormatter.ISO_LOCAL_DATE);
                    outboundJourney.setArrivalTime(arrivalDate.atStartOfDay());
                } catch (Exception e) {
                    log.warn("抵达时间解析失败：{}", requirements.getArrivalDate());
                }
            }

            outboundJourney.setTripType(TripType.OUTBOUND); // 去程
            outboundJourney.setCreateTime(LocalDateTime.now());
            outboundJourney.setUpdateTime(LocalDateTime.now());

            setFlightInfo(order, outboundJourney, requirements);

            journeys.add(outboundJourney);
        }

        // 返程（如果有）
        if (StringUtils.isNotBlank(requirements.getReturnDate())) {
            try {
                LocalDate returnDate = LocalDate.parse(requirements.getReturnDate(),
                        DateTimeFormatter.ISO_LOCAL_DATE);

                Journey returnJourney = new Journey();
                returnJourney.setCompanyId(userHolder.getCompanyId());
                returnJourney.setDeparture(requirements.getArrivalCity());
                returnJourney.setArrival(requirements.getDepartureCity());
                returnJourney.setDepartureTime(returnDate.atStartOfDay());
                returnJourney.setTripType(TripType.RETURN); // 返程ss
                returnJourney.setCreateTime(LocalDateTime.now());
                returnJourney.setUpdateTime(LocalDateTime.now());

                setFlightInfo(order, returnJourney, requirements);

                journeys.add(returnJourney);

            } catch (Exception e) {
                log.warn("返回时间解析失败：{}", requirements.getReturnDate());
            }
        }

        return journeys;
    }

    private void setFlightInfo(Order order, Journey journey, OrderRequirements requirements) {
        if (!Scene.FLIGHT.equals(order.getOrderType())) {
            return;
        }
        if (StringUtils.isNotBlank(requirements.getFlightNo())) {
            journey.setFlightNo(requirements.getFlightNo());
        }
        if (StringUtils.isNotBlank(requirements.getAirline())) {
            journey.setAirline(requirements.getAirline());
        }
        if (StringUtils.isNotBlank(requirements.getCabinClass())) {
            journey.setCabinClass(requirements.getCabinClass());
        }
    }

    /**
     * 将字符串转换为 Scene 枚举
     */
    private Scene convertToScene(String orderType) {
        if (StringUtils.isBlank(orderType)) {
            return Scene.QA;
        }

        try {
            return Scene.valueOf(orderType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的订单类型：{}，使用 QA 作为默认值", orderType);
            return Scene.QA;
        }
    }
}
