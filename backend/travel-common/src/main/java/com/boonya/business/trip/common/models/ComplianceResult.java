package com.boonya.business.trip.common.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 合规检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceResult {

    /**
     * 是否合规
     */
    private Boolean compliant;

    /**
     * 违规原因（如果不合规）
     */
    private String violationReason;

    /**
     * 违规详情列表（可能有多个违规项）
     */
    @Builder.Default
    private List<String> violationDetails = new ArrayList<>();

    /**
     * 建议的替代方案
     */
    private String suggestion;

    /**
     * 差标依据说明
     */
    private String standardReference;

    /**
     * 是否需要特殊审批
     */
    private Boolean requiresSpecialApproval;

    /**
     * 添加违规详情
     */
    public void addViolationDetail(String detail) {
        if (this.violationDetails == null) {
            this.violationDetails = new ArrayList<>();
        }
        this.violationDetails.add(detail);
    }

    public ComplianceResult(Boolean compliant, String violationReason) {
        this.compliant = compliant;
        this.violationReason = violationReason;
    }

    public static ComplianceResult compliant(String reason) {
        return ComplianceResult.builder()
                .compliant(true)
                .violationReason(reason)
                .build();
    }
}
