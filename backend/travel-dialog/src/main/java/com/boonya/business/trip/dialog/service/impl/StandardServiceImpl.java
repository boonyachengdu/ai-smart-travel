package com.boonya.business.trip.dialog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.Standard;
import com.boonya.business.trip.dialog.mapper.StandardMapper;
import com.boonya.business.trip.dialog.service.StandardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardServiceImpl extends ServiceImpl<StandardMapper, Standard> implements StandardService {
    private final StandardMapper standardMapper;

    @Override
    public Standard getStandard(Long companyId) {
        if (companyId == null) {
            log.warn("公司 ID 为空，返回默认差标");
            return createDefaultStandard();
        }

        try {
            // 查询公司的差旅标准
            LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Standard::getCompanyId, companyId)
                    .eq(Standard::getDeleted, 0)
                    .orderByDesc(Standard::getUpdateTime)
                    .last("LIMIT 1");

            Standard standard = getOne(wrapper);

            if (standard == null) {
                log.info("公司 {} 未配置差标，使用默认标准", companyId);
                return createDefaultStandard();
            }

            log.debug("获取公司 {} 的差标：{}", companyId, standard.getName());
            return standard;

        } catch (Exception e) {
            log.error("获取差标失败，公司 ID: {}", companyId, e);
            return createDefaultStandard();
        }
    }

    @Override
    public Boolean batchInit(Long companyId){
        try {
            // 检查是否已存在数据
            LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Standard::getCompanyId, companyId);
            Long count = standardMapper.selectCount(wrapper);

            if (count > 0) {
                return true;
            }

            // 初始化 5 条差标数据
            List<Standard> standards = List.of(
                    createStandard(companyId, "普通员工差标", "适用于普通员工", 1500.0, 400.0, 600.0, 150.0, 8000.0, 50000.0),
                    createStandard(companyId, "中层管理人员差标", "适用于中层管理", 3000.0, 800.0, 1000.0, 300.0, 15000.0, 100000.0),
                    createStandard(companyId, "高级管理人员差标", "适用于高管", 8000.0, 1500.0, 2000.0, 500.0, 30000.0, 200000.0),
                    createStandard(companyId, "特殊项目差标", "特殊任务", 5000.0, 1000.0, 1200.0, 400.0, 20000.0, 150000.0),
                    createStandard(companyId, "实习生差标", "实习生", 800.0, 200.0, 300.0, 100.0, 3000.0, 20000.0)
            );

            return this.saveBatch(standards);
        } catch (Exception e) {
            log.error("批量初始化差标失败", e);
            return false;
        }
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

        List<Standard> standards = this.list(wrapper);
        log.info("【查询差标】企业 {} 共有 {} 条差标标准", companyId, standards.size());

        return standards;
    }

    private Standard createStandard(Long companyId, String name, String desc,
                                    Double flight, Double hotel, Double train,
                                    Double car, Double month, Double year) {
        Standard standard = new Standard();
        standard.setCompanyId(companyId);
        standard.setName(name);
        standard.setDescription(desc);
        standard.setFlightMaxPrice(flight);
        standard.setHotelMaxPrice(hotel);
        standard.setTrainMaxPrice(train);
        standard.setCarMaxPrice(car);
        standard.setMonthMaxPrice(month);
        standard.setYearMaxPrice(year);
        return standard;
    }

    private Standard createDefaultStandard() {
        Standard standard = new Standard();
        standard.setName("默认标准");
        standard.setDescription("默认标准");
        standard.setFlightMaxPrice(1500.0);
        standard.setHotelMaxPrice(500.0);
        standard.setTrainMaxPrice(150.0);
        standard.setCarMaxPrice(8000.0);
        standard.setMonthMaxPrice(50000.0);
        standard.setYearMaxPrice(200000.0);
        return standard;
    }
}
