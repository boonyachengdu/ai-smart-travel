package com.boonya.business.trip.dialog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.entity.Standard;
import com.boonya.business.trip.common.models.ComplianceResult;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.dialog.mapper.StandardMapper;
import com.boonya.business.trip.dialog.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final StandardMapper standardMapper;

    @Override
    public ComplianceResult validateByStandard(OrderRequirements order, Long companyId) {
        log.info("【差标校验】开始校验订单，companyId={}, orderType={}", companyId, order.getOrderType());

        if (companyId == null) {
            log.warn("【差标校验】企业 ID 为空，放行");
            return new ComplianceResult(true, "企业 ID 为空，自动通过");
        }

        // 1. 查询企业的差标标准（取最新的）
        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Standard::getCompanyId, companyId);
        wrapper.eq(Standard::getDeleted, 0);
        wrapper.orderByDesc(Standard::getUpdateTime);
        List<Standard> standards = standardMapper.selectList(wrapper);

        if (standards.isEmpty()) {
            log.warn("【差标校验】企业 {} 未设置差标标准，放行", companyId);
            return new ComplianceResult(true, "企业未设置差标标准，自动通过");
        }

        // 使用第一条差标标准（可根据用户角色匹配更精确的标准）
        Standard standard = standards.get(0);
        log.info("【差标校验】使用差标标准：{} - {}", standard.getId(), standard.getName());

        // 2. 获取订单预算
        String orderType = order.getOrderType();
        Double budget = order.getBudget();

        if (!StringUtils.hasText(orderType)) {
            log.warn("【差标校验】订单类型为空，跳过校验");
            return new ComplianceResult(true, "订单类型为空，自动通过");
        }

        if (budget == null || budget <= 0) {
            log.warn("【差标校验】订单预算无效：{}, 跳过校验", budget);
            return new ComplianceResult(true, "订单预算为空，自动通过");
        }

        // 3. 按场景校验价格上限
        boolean violated = false;
        String violationReason = null;

        switch (orderType.toUpperCase()) {
            case "FLIGHT":
                if (standard.getFlightMaxPrice() != null &&
                        budget.compareTo(standard.getFlightMaxPrice()) > 0) {
                    violated = true;
                    violationReason = String.format("机票预算 %.2f 元超过差标上限 %.2f 元",
                            budget, standard.getFlightMaxPrice());
                }
                break;

            case "HOTEL":
                if (standard.getHotelMaxPrice() != null &&
                        budget.compareTo(standard.getHotelMaxPrice()) > 0) {
                    violated = true;
                    violationReason = String.format("酒店预算 %.2f 元/晚超过差标上限 %.2f 元/晚",
                            budget, standard.getHotelMaxPrice());
                }
                break;

            case "TRAIN":
                if (standard.getTrainMaxPrice() != null &&
                        budget.compareTo(standard.getTrainMaxPrice()) > 0) {
                    violated = true;
                    violationReason = String.format("火车预算 %.2f 元超过差标上限 %.2f 元",
                            budget, standard.getTrainMaxPrice());
                }
                break;

            case "CAR":
                if (standard.getCarMaxPrice() != null &&
                        budget.compareTo(standard.getCarMaxPrice()) > 0) {
                    violated = true;
                    violationReason = String.format("用车预算 %.2f 元超过差标上限 %.2f 元",
                            budget, standard.getCarMaxPrice());
                }
                break;

            default:
                log.info("【差标校验】未知订单类型 {}，跳过校验", orderType);
                return new ComplianceResult(true, "未知订单类型，自动通过");
        }

        // 4. 返回校验结果
        if (violated) {
            log.warn("【差标校验】违规：{}", violationReason);
            return new ComplianceResult(false, violationReason);
        }

        log.info("【差标校验】订单符合差标要求");
        return new ComplianceResult(true, "订单符合差标规定");
    }

    @Override
    public List<Standard> getStandardsByCompanyId(Long companyId) {
        if (companyId == null) {
            log.warn("【查询差标】企业 ID 为空");
            return List.of();
        }

        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Standard::getCompanyId, companyId);
        wrapper.eq(Standard::getDeleted, 0);
        wrapper.orderByDesc(Standard::getUpdateTime);
        return standardMapper.selectList(wrapper);
    }

    @Override
    public Standard getStandardByScene(Long companyId, String scene) {
        if (companyId == null || !StringUtils.hasText(scene)) {
            log.warn("【查询差标】参数为空：companyId={}, scene={}", companyId, scene);
            return null;
        }

        List<Standard> standards = getStandardsByCompanyId(companyId);
        if (standards.isEmpty()) {
            return null;
        }

        // TODO: 根据场景和用户角色智能匹配差标标准
        // 目前默认返回第一个（可后续扩展为根据员工级别匹配）
        return standards.get(0);
    }
}
