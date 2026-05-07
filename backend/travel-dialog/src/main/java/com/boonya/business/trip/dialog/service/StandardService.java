package com.boonya.business.trip.dialog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.entity.Standard;

import java.util.List;

/**
 * 差标服务
 */
public interface StandardService extends IService<Standard> {

    Boolean batchInit(Long companyId);

    List<Standard> getStandardsByCompanyId(Long companyId);

    /**
     * 根据公司 ID 获取差旅标准
     *
     * @param companyId 公司 ID
     * @return 差旅标准，如果不存在则返回默认标准
     */
    Standard getStandard(Long companyId);

    /**
     * 获取机票最高价格标准
     *
     * @param companyId 公司 ID
     * @return 机票最高价格
     */
    default Double getFlightMaxPrice(Long companyId) {
        Standard standard = getStandard(companyId);
        return standard != null ? standard.getFlightMaxPrice() : 1500.0;
    }

    /**
     * 获取酒店最高价格标准
     *
     * @param companyId 公司 ID
     * @return 酒店最高价格
     */
    default Double getHotelMaxPrice(Long companyId) {
        Standard standard = getStandard(companyId);
        return standard != null ? standard.getHotelMaxPrice() : 500.0;
    }
}
