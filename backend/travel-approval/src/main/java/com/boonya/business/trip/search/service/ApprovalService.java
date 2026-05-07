package com.boonya.business.trip.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.models.approval.AuditRequest;

import java.util.List;

public interface ApprovalService extends IService<Approval> {

    Boolean updateApproveOrReject(Approval approval);

    Boolean updateApproveOrRejectCallback(Approval approval);

    Long pendingApprovals();

    List<Approval> getUnApprovalList();
}
