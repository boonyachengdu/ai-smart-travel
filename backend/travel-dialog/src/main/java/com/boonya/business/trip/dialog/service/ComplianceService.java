package com.boonya.business.trip.dialog.service;

import com.boonya.business.trip.common.entity.Standard;
import com.boonya.business.trip.common.models.ComplianceResult;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;

import java.util.List;

/**
 * 合规性校验服务接口
 */
public interface ComplianceService {

    /**
     * 根据差标标准验证订单
     *
     * @param order 订单需求
     * @param companyId 企业 ID
     * @return 合规性结果
     */
    ComplianceResult validateByStandard(OrderRequirements order, Long companyId);

    /**
     * 根据企业 ID 查询差标标准列表
     *
     * @param companyId 企业 ID
     * @return 差标标准列表
     */
    List<Standard> getStandardsByCompanyId(Long companyId);

    /**
     * 根据企业 ID 和场景查询差标标准
     *
     * @param companyId 企业 ID
     * @param scene 场景（FLIGHT/HOTEL/TRAIN/CAR）
     * @return 差标标准
     */
    Standard getStandardByScene(Long companyId, String scene);
}
