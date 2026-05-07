package com.boonya.business.trip.common.models.approval;

import com.boonya.business.trip.common.entity.Approval;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalCreateRequest {

    /**
     * 审批单基本信息
     */
    private Approval approval;

    /**
     * 出行项列表
     */
    private List<ApprovalTripItem> tripItems;
}