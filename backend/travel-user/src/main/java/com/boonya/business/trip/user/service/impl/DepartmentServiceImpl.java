package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.Department;
import com.boonya.business.trip.user.mapper.DepartmentMapper;
import com.boonya.business.trip.user.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {
    private final DepartmentMapper departmentMapper;
    @Override
    public List<Department> listByCompanyId(Long companyId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeleted, false);
        wrapper.eq(Department::getCompanyId, companyId);
        wrapper.orderByAsc(Department::getId);
        return departmentMapper.selectList(wrapper);
    }
}
