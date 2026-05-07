package com.boonya.business.trip.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    List<Department> listByCompanyId(Long companyId);

}
