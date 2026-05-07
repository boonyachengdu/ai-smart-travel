package com.boonya.business.trip.order.service;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Journey;
import com.boonya.business.trip.common.entity.Order;
import com.boonya.business.trip.common.models.approval.ApprovalTripItem;
import com.boonya.business.trip.common.utils.IdGenerator;
import com.boonya.business.trip.feign.clients.ApprovalFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowService {

    private final ApprovalFeignClient approvalFeignClient;

    /**
     * 判断订单是否需要审批
     */
    public boolean needApproval(Order order, Company company) {
        // 1. 金额超过阈值需要审批
        if (order.getAmount().compareTo(company.getOverAmountMustAudit()) > 0) {
            log.info("订单金额 {} 超过阈值 {}，需要审批",
                    order.getAmount(), company.getOverAmountMustAudit());
            return true;
        }

        // 2. 企业配置强制审批
        if (Boolean.FALSE.equals(company.getAutoApprove())) {
            log.info("企业配置强制审批，需要审批");
            return true;
        }

        // 3. 混合订单需要审批
        if (isMixedOrder(order)) {
            log.info("混合订单类型，需要审批");
            return true;
        }

        return false;
    }

    /**
     * 判断是否为混合订单
     */
    public boolean isMixedOrder(Order order) {
        if (ObjectUtils.isEmpty(order.getJourney()) || order.getJourney().size() <= 1) {
            return false;
        }

        // 检查是否有不同类型的行程
        Scene firstType = order.getOrderType();
        for (Journey journey : order.getJourney()) {
            if (!firstType.equals(order.getOrderType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建审批流程
     */
    public Approval createApprovalFlow(Company company, Order order, UserHolder userContext) {
        String reason = buildApprovalReason(order, company);

        Approval approval = new Approval();
        approval.setOrderNo(order.getOrderNo());
        approval.setCompanyId(order.getCompanyId());
        approval.setApplicantId(userContext.getEmployeeId());
        approval.setStatus(AuditStatus.PENDING);
        approval.setReason(reason);
        approval.setRemark("系统自动创建审批");
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        // 构造审批出行项
        List<ApprovalTripItem> tripItems = buildApprovalTripItems(order, reason);
        approval.setTripItems(tripItems);

        // 保存审批申请
        Response<Long> saveResponse = approvalFeignClient.save(approval);
        if (saveResponse.isSuccess() && saveResponse.getData() != null) {
            approval.setId(saveResponse.getData());
            log.info("审批申请保存成功，审批 ID: {}", approval.getId());
        }

        return approval;
    }

    /**
     * 构建审批原因
     */
    public String buildApprovalReason(Order order, Company company) {
        StringBuilder reason = new StringBuilder();
        // 场景下单
        reason.append(order.getOrderType().getDesc().substring(0, 2)).append(order.getOrderNo());

        // 金额超限
        if (order.getAmount().compareTo(company.getOverAmountMustAudit()) > 0) {
            reason.append(" [金额超限：")
                    .append(order.getAmount())
                    .append(" > ")
                    .append(company.getOverAmountMustAudit())
                    .append("]");
        }

        // 混合订单
        if (isMixedOrder(order)) {
            reason.append(" [混合订单]");
        }

        return reason.toString();
    }

    /**
     * 构建审批出行项
     */
    public List<ApprovalTripItem> buildApprovalTripItems(Order order, String reason) {
        List<ApprovalTripItem> tripItems = new ArrayList<>();

        if (!ObjectUtils.isEmpty(order.getJourney())) {
            for (Journey journey : order.getJourney()) {
                ApprovalTripItem tripItem = new ApprovalTripItem();
                tripItem.setUuid(IdGenerator.getUuid());
                tripItem.setTripType(order.getOrderType());
                tripItem.setTripName(reason);
                tripItem.setDeparture(journey.getDeparture());
                tripItem.setArrival(journey.getArrival());
                tripItem.setStartTime(journey.getDepartureTime());
                tripItem.setEndTime(journey.getArrivalTime());
                tripItems.add(tripItem);
            }
        }

        return tripItems;
    }
}
