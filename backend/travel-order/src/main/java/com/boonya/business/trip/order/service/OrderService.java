package com.boonya.business.trip.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;

public interface OrderService extends IService<Order> {

    OrderGenerationResponse createOrder(OrderRequirements requirements);

    Order getByApprovalId(Long approvalId);

    Boolean approved(Order order, String remark);

    Boolean approvedCallback(Order order, String remark);

    Boolean rejected(Order order, String reason);

    Boolean rejectedCallback(Order order, String reason);

    Long totalOrders();

    Long monthOrders();
}