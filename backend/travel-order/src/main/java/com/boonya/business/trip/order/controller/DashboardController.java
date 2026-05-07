package com.boonya.business.trip.order.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.feign.clients.ApprovalFeignClient;
import com.boonya.business.trip.feign.clients.UserFeignClient;
import com.boonya.business.trip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/order/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final OrderService orderService;
    private final ApprovalFeignClient approvalFeignClient;
    private final UserFeignClient userFeignClient;

    /**
     * 超级管理员仪表盘
     * @return
     */
    @GetMapping("/global")
    public Response<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", orderService.totalOrders());
        data.put("monthOrders", orderService.monthOrders());
        data.put("pendingApprovals", approvalFeignClient.pendingApprovals().getData());
        data.put("totalUsers", userFeignClient.countUsers().getData());
        return Response.ok(data);
    }

}
