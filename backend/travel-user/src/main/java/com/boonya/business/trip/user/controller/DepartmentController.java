package com.boonya.business.trip.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Department;
import com.boonya.business.trip.common.models.user.request.DepartmentQueryRequest;
import com.boonya.business.trip.common.models.user.request.DepartmentTreeRequest;
import com.boonya.business.trip.user.service.DepartmentCacheService;
import com.boonya.business.trip.user.service.DepartmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final DepartmentCacheService departmentCacheService;

    /**
     * 分页查询部门列表（支持关键字搜索）
     */
    @PostMapping("/page")
    public Response<Page<Department>> page(@RequestBody DepartmentQueryRequest request) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Department::getCompanyId, userHolder.getCompanyId());
        }

        if (StringUtils.hasText(request.getName())) {
            wrapper.like(Department::getName, request.getName());
        }
        if (request.getCompanyId() != null) {
            wrapper.eq(Department::getCompanyId, request.getCompanyId());
        }

        wrapper.orderByAsc(Department::getId); // 可根据业务调整排序

        Page<Department> page = departmentService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 获取部门树形结构（前端树组件使用）
     */
    @PostMapping("/tree")
    public Response<List<Department>> tree(@RequestBody DepartmentTreeRequest request) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Department::getCompanyId, userHolder.getCompanyId());
        }

        if (request.getCompanyId() != null) {
            wrapper.eq(Department::getCompanyId, request.getCompanyId());
        }
        wrapper.orderByAsc(Department::getParentId, Department::getId);

        List<Department> list = departmentService.list(wrapper);
        return Response.ok(list);
    }

    /**
     * 根据ID查询单个部门
     */
    @GetMapping("/{id}")
    public Response<Department> getById(@PathVariable("id") Long id) {
        Department dept = departmentService.getById(id);
        return dept != null ? Response.ok(dept) : Response.error("部门不存在");
    }

    /**
     * 新增部门
     */
    @PostMapping
    public Response<Long> save(@RequestBody Department department) {
        boolean success = departmentService.save(department);
        if (success) {
            departmentCacheService.cacheDepartmentName(department.getId(), department.getName());
        }
        return success ? Response.ok(department.getId()) : Response.error("新增失败");
    }

    /**
     * 修改部门
     */
    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Department department) {
        department.setId(id);
        boolean success = departmentService.updateById(department);
        if (success) {
            departmentCacheService.cacheDepartmentName(department.getId(), department.getName());
        }
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 删除部门（逻辑删除，注意：有子部门或员工时需业务校验）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        // 实际项目建议先校验是否有子部门或员工
        boolean success = departmentService.removeById(id);
        if (success) {
            departmentCacheService.removeDepartmentCache(id);
        }
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @PostConstruct
    @Async
    public void init() {
        // 缓存部门名称
        List<Department> list = departmentService.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(dept -> departmentCacheService.cacheDepartmentName(dept.getId(), dept.getName()));
        }
        log.info("部门缓存初始化完成");
    }

    @GetMapping("list")
    public Response<List<Department>> list() {
        UserHolder userHolder = UserHolder.get();
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeleted, 0);
        wrapper.eq(Department::getCompanyId, userHolder.getCompanyId());
        wrapper.orderByAsc(Department::getId);

        List<Department> list = departmentService.listByCompanyId(userHolder.getCompanyId());
        return Response.ok(list);
    }
}