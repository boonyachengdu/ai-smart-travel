package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.user.mapper.CompanyMapper;
import com.boonya.business.trip.user.service.CompanyCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CompanyCacheServiceImpl implements CompanyCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CompanyMapper companyMapper;

    private static final String COMPANY_KEY_PREFIX = "cache:company:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void cacheCompanyName(Long companyId, String companyName) {
        if (companyId == null || companyName == null) {
            return;
        }
        String key = COMPANY_KEY_PREFIX + companyId;
        redisTemplate.opsForValue().set(key, companyName, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void cacheCompanyNames(Collection<Long> companyIds) {
        if (CollectionUtils.isEmpty(companyIds)) {
            return;
        }

        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Company::getId, companyIds);
        List<Company> companies = companyMapper.selectList(wrapper);

        for (Company company : companies) {
            cacheCompanyName(company.getId(), company.getName());
        }
    }

    @Override
    public void removeCompanyCache(Long companyId) {
        if (companyId == null) {
            return;
        }
        String key = COMPANY_KEY_PREFIX + companyId;
        redisTemplate.delete(key);
    }
}
