package com.boonya.business.trip.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.models.approval.AuditRequest;
import com.boonya.business.trip.feign.clients.OrderFeignClient;
import com.boonya.business.trip.search.mapper.ApprovalMapper;
import com.boonya.business.trip.search.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {
    private final ApprovalMapper approvalMapper;
    private final OrderFeignClient orderFeignClient;

    @Transactional
    @Override
    public Boolean updateApproveOrReject(Approval approval) {
        AuditStatus status = approval.getStatus();
        AuditRequest request = new AuditRequest(approval.getId(), approval.getRemark());
        if (AuditStatus.APPROVED.equals(status)) {
            // 调用订单服务 先调用对向服务再调用自己
            Response<?> response = orderFeignClient.approveCallback(request);
            if (response.isSuccess()) {
                log.info("订单服务调用审批服务成功");
                updateApproveOrRejectCallback(approval);
                return true;
            } else {
                log.error("订单服务调用失败:{}", response.getMessage());
                return false;
            }
        }

        if (AuditStatus.REJECTED.equals(status)) {
            // 调用订单服务 先调用对向服务再调用自己
            Response<?> response = orderFeignClient.rejectCallback(request);
            if (response.isSuccess()) {
                log.info("订单服务调用审批服务成功");
                updateApproveOrRejectCallback(approval);
                return true;

            } else {
                log.error("订单服务调用失败:{}", response.getMessage());
                return false;
            }
        }
        return false;
    }

    @Override
    public Boolean updateApproveOrRejectCallback(Approval approval) {
        UserHolder userHolder = UserHolder.get();
        approval.setUpdateTime(LocalDateTime.now());
        approval.setApproverId(userHolder.getEmployeeId());
        baseMapper.updateById(approval);
        return true;
    }

    @Override
    public Long pendingApprovals() {
        UserHolder userHolder = UserHolder.get();
        if (userHolder.isSuperAdmin()) {
            return baseMapper.countPendingAll();
        } else {
            return baseMapper.countPendingByCompanyId(userHolder.getCompanyId());
        }
    }

    @Override
    public List<Approval> getUnApprovalList() {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isAuditor()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Approval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Approval::getDeleted, 0);
        wrapper.eq(Approval::getCompanyId, userHolder.getCompanyId());
        wrapper.eq(Approval::getStatus, AuditStatus.PENDING);
        wrapper.orderByDesc(Approval::getUpdateTime);
        return approvalMapper.selectList(wrapper);
    }
}
