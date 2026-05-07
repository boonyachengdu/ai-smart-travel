package com.boonya.business.trip.user.service;

import java.util.Collection;

public interface CompanyCacheService {

    /**
     * 缓存企业名称
     */
    void cacheCompanyName(Long companyId, String companyName);

    /**
     * 批量缓存企业名称
     */
    void cacheCompanyNames(Collection<Long> companyIds);

    /**
     * 删除企业缓存
     */
    void removeCompanyCache(Long companyId);
}
