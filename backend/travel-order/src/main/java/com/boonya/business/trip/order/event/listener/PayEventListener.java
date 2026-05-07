package com.boonya.business.trip.order.event.listener;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.OrderStatus;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.order.event.PayEvent;
import com.boonya.business.trip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayEventListener implements ApplicationListener<PayEvent> {
    private final OrderService orderService;
    //private final PaymentFeignClient paymentFeignClient;

    @Value("${channel.data.mock:true}")
    private boolean mockChannelData;

    @Override
    public void onApplicationEvent(PayEvent event) {
        Order order = (Order) event.getSource();
        try {
            Response<Boolean> paymentResponse = initiatePayment(order);
            if (!paymentResponse.isSuccess()) {
                log.warn("发起支付失败，但审批已通过：orderNo={}", order.getOrderNo());
            }
        } catch (Exception e) {
            log.error("支付流程异常：orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 发起支付流程
     */
    private Response<Boolean> initiatePayment(Order order) {
        try {
            log.info("开始发起支付流程：orderNo={}, amount={}", order.getOrderNo(), order.getAmount());

            // 构建支付请求参数
            Map<String, Object> paymentParams = new HashMap<>();
            paymentParams.put("orderNo", order.getOrderNo());
            paymentParams.put("amount", order.getAmount());
            paymentParams.put("userId", order.getUserId());
            paymentParams.put("companyId", order.getCompanyId());
            paymentParams.put("paymentType", "CORPORATE"); // 企业支付

            if (mockChannelData) {
                return paySuccess(order);
            } else {
                return payFailed(order, Response.ok());
//                // 调用支付服务（如果支付服务不可用，记录日志但不影响流程）
//                Response<Boolean> response = paymentFeignClient.createPayment(paymentParams);
//
//                if (response.isSuccess()) {
//                    return paySuccess(order);
//                } else {
//                    return payFailed(order, response);
//                }
            }

        } catch (Exception e) {
            log.error("发起支付流程异常：orderNo={}", order.getOrderNo(), e);

            // 异常情况下设置为待支付状态，等待后续处理
            try {
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                order.setUpdateTime(LocalDateTime.now());
                orderService.updateById(order);
            } catch (Exception ex) {
                log.error("更新订单状态失败", ex);
            }

            return Response.error("发起支付失败：" + e.getMessage());
        }
    }

    @NotNull
    private Response<Boolean> paySuccess(Order order) {
        log.info("支付流程发起成功：orderNo={}", order.getOrderNo());

        // 更新订单状态为待支付
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);

        return Response.ok(true);
    }

    @NotNull
    private Response<Boolean> payFailed(Order order, Response<Boolean> response) {
        log.warn("支付服务调用失败，但订单已进入待支付状态：orderNo={}, error={}",
                order.getOrderNo(), response.getMessage());

        // 即使支付服务失败，也更新订单状态
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);

        return Response.ok(false);
    }
}
