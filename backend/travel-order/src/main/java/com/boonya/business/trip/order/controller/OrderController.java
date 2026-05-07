package com.boonya.business.trip.order.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.constant.OrderStatus;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.approval.AuditRequest;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.common.models.order.request.OrderQueryRequest;
import com.boonya.business.trip.order.event.PayEvent;
import com.boonya.business.trip.order.service.ApprovalFlowService;
import com.boonya.business.trip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ApprovalFlowService approvalFlowService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 分页查询订单列表
     */
    @PostMapping("/page")
    public Response<Page<Order>> page(@RequestBody OrderQueryRequest request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getDeleted, 0);
        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()) {
            wrapper.eq(Order::getCompanyId, userHolder.getCompanyId());
        }

        if (StringUtils.hasText(request.getOrderNo())) {
            wrapper.like(Order::getOrderNo, request.getOrderNo());
        }
        if (StringUtils.hasText(request.getOrderType())) {
            wrapper.eq(Order::getOrderType, request.getOrderType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Order::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getAuditStatus())) {
            wrapper.eq(Order::getAuditStatus, request.getAuditStatus());
        }
        if (request.getUserId() != null) {
            wrapper.eq(Order::getUserId, request.getUserId());
        }
        if (request.getCompanyId() != null) {
            wrapper.eq(Order::getCompanyId, request.getCompanyId());
        }

        wrapper.orderByDesc(Order::getUpdateTime);

        Page<Order> page = orderService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询订单详情（包含关联行程和票务）
     */
    @GetMapping("/{id}")
    public Response<Order> getById(@PathVariable("id") Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Response.error("订单不存在");
        }

        // 可选：关联查询 journeys 和 tickets（实际可使用 MyBatis-Plus 的关联查询或 Service 层处理）
        // 这里简化返回订单主信息，前端可单独请求 /journeys 和 /tickets
        return Response.ok(order);
    }

    /**
     * 删除订单（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = orderService.removeById(id);
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    /**
     * 创建订单（RAG校验过的订单参数）
     */
    @PostMapping("/createOrder")
    public Response<OrderGenerationResponse> createOrder(@RequestBody OrderRequirements requirements) {
        try {
            if (requirements == null) {
                return Response.error("订单生成参数不能为空");
            }
            log.info("订单生成参数：{}", JSONObject.toJSONString(requirements));
            // 此处不做校验了，直接根据orderType生成订单
            return Response.ok(orderService.createOrder(requirements));
        } catch (Exception e) {
            log.error("订单生成失败", e);
            return Response.error("订单生成失败：" + e.getMessage());
        }
    }

    /**
     * 审批通过回调接口（供审批模块调用）
     */
    @PostMapping("/callback/approve")
    public Response<Map<String, Object>> approveCallback(@RequestBody AuditRequest request) {
        try {
            Order order = orderService.getByApprovalId(request.getApprovalId());
            if (order == null) {
                return Response.error("订单不存在");
            }

            orderService.approvedCallback(order, request.getRemark());

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("status", AuditStatus.APPROVED.name());
            result.put("message", "审批通过回调成功，已发起支付流程");

            return Response.ok(result);

        } catch (Exception e) {
            log.error("审批通过回调失败", e);
            return Response.error("审批通过回调失败：" + e.getMessage());
        }
    }

    /**
     * 审批拒绝回调接口（供审批模块调用）
     */
    @PostMapping("/callback/reject")
    public Response<Map<String, Object>> rejectCallback(@RequestBody AuditRequest request) {
        try {
            Order order = orderService.getByApprovalId(request.getApprovalId());
            if (order == null) {
                return Response.error("订单不存在");
            }

            orderService.rejectedCallback(order, request.getRemark());

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("status", AuditStatus.REJECTED.name());
            result.put("message", "审批拒绝回调成功，订单已作废");

            return Response.ok(result);

        } catch (Exception e) {
            log.error("审批拒绝回调失败", e);
            return Response.error("审批拒绝回调失败：" + e.getMessage());
        }
    }

    /**
     * 根据订单号查询订单
     */
    @GetMapping("/by-order-no/{orderNo}")
    public Response<Order> getByOrderNo(@PathVariable("orderNo") String orderNo) {
        try {
            List<Order> orders = orderService.list();
            Order order = orders.stream()
                    .filter(o -> o.getOrderNo().equals(orderNo))
                    .findFirst()
                    .orElse(null);

            if (order == null) {
                return Response.error("订单不存在");
            }

            return Response.ok(order);
        } catch (Exception e) {
            log.error("查询订单失败", e);
            return Response.error("查询订单失败：" + e.getMessage());
        }
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve/{approvalId}")
    public Response<Map<String, Object>> approve(@PathVariable("approvalId") Long approvalId,
                                                 @RequestBody Map<String, String> params) {
        try {
            UserHolder userContext = UserHolder.get();
            if (userContext == null || userContext.getUserId() == null) {
                return Response.error("用户未登录");
            }

            String remark = params.getOrDefault("remark", "审批通过");

            if (!userContext.isAuditor() && !userContext.isAdmin()) {
                return Response.error("无审批权限");
            }

            Order order = orderService.getByApprovalId(approvalId);
            if (order == null) {
                return Response.error("订单不存在");
            }

            boolean success = orderService.approved(order, remark);
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("approvalId", approvalId);
                result.put("status", AuditStatus.APPROVED.name());
                result.put("message", "审批通过，已发起支付流程");
                return Response.ok(result);
            }
            return Response.error("审批通过失败");
        } catch (Exception e) {
            log.error("审批通过失败", e);
            return Response.error("审批通过失败：" + e.getMessage());
        }
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/reject/{approvalId}")
    public Response<Map<String, Object>> reject(@PathVariable("approvalId") Long approvalId,
                                                @RequestBody Map<String, String> params) {
        try {
            UserHolder userContext = UserHolder.get();
            if (userContext == null || userContext.getUserId() == null) {
                return Response.error("用户未登录");
            }

            if (!userContext.isAuditor() && !userContext.isAdmin()) {
                return Response.error("无审批权限");
            }

            Order order = orderService.getByApprovalId(approvalId);
            if (order == null) {
                return Response.error("订单不存在");
            }

            String reason = params.getOrDefault("remark", "审批拒绝");
            boolean success = orderService.rejected(order, reason);
            if (success){
                Map<String, Object> result = new HashMap<>();
                result.put("approvalId", approvalId);
                result.put("status", AuditStatus.REJECTED.name());
                result.put("message", "审批拒绝，订单已作废");
                return Response.ok(result);
            }
            return Response.error("审批拒绝失败");
        } catch (Exception e) {
            log.error("审批拒绝失败", e);
            return Response.error("审批拒绝失败：" + e.getMessage());
        }
    }
}
