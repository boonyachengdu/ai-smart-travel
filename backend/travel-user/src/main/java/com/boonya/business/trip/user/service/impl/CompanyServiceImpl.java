package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.user.mapper.CompanyMapper;
import com.boonya.business.trip.user.service.CompanyService;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

}
