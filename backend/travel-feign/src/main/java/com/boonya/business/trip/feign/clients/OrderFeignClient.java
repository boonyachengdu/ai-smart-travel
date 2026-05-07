package com.boonya.business.trip.feign.clients;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.approval.AuditRequest;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        name = "travel-order",
        path = "/order",
        configuration = FeignClientConfig.class
)
public interface OrderFeignClient {

    /**
     * 创建订单
     */
    @PostMapping("/createOrder")
    public Response<OrderGenerationResponse> createOrder(@RequestBody OrderRequirements orderData);

    /**
     * 根据订单号查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Response<Order> getByOrderNo(@PathVariable("orderNo") String orderNo);

    /**
     * 审批通过回调接口（供审批模块调用）
     */
    @PostMapping("/callback/approve")
    public Response<Map<String, Object>> approveCallback(@RequestBody AuditRequest request);

    /**
     * 审批拒绝回调接口（供审批模块调用）
     */
    @PostMapping("/callback/reject")
    public Response<Map<String, Object>> rejectCallback(@RequestBody AuditRequest request);
}
