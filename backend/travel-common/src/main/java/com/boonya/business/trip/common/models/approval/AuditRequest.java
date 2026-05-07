package com.boonya.business.trip.common.models.approval;

import com.boonya.business.trip.common.constant.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequest {
    private Long approvalId;
    private String remark;
}
