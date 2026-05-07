package com.boonya.business.trip.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.request.EmployeeQueryRequest;
import com.boonya.business.trip.user.service.EmployeeCacheService;
import com.boonya.business.trip.user.service.EmployeeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeCacheService employeeCacheService;

    /**
     * 分页查询员工列表
     */
    @PostMapping("/page")
    public Response<Page<Employee>> page(@RequestBody EmployeeQueryRequest request) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Employee::getCompanyId, userHolder.getCompanyId());
        }

        if (StringUtils.hasText(request.getEmployeeNo())) {
            wrapper.like(Employee::getEmployeeNo, request.getEmployeeNo());
        }
        if (StringUtils.hasText(request.getPosition())) {
            wrapper.like(Employee::getPosition, request.getPosition());
        }
        if (request.getCompanyId() != null) {
            wrapper.eq(Employee::getCompanyId, request.getCompanyId());
        }
        if (request.getDepartmentId() != null) {
            wrapper.eq(Employee::getDepartmentId, request.getDepartmentId());
        }

        wrapper.orderByDesc(Employee::getUpdateTime);

        Page<Employee> page = employeeService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询单个员工
     */
    @GetMapping("/{id}")
    public Response<Employee> getById(@PathVariable("id") Long id) {
        Employee employee = employeeService.getById(id);
        return employee != null ? Response.ok(employee) : Response.error("员工不存在");
    }

    /**
     * 新增员工
     */
    @PostMapping
    public Response<Long> save(@RequestBody Employee employee) {
        // 业务校验（例如 user_id 是否已关联其他员工）
        boolean success = employeeService.save(employee);
        if (success) {
            employeeCacheService.cacheEmployeeName(employee.getUserId(), employee.getName());
        }
        return success ? Response.ok(employee.getId()) : Response.error("新增失败");
    }

    /**
     * 修改员工
     */
    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Employee employee) {
        employee.setId(id);
        boolean success = employeeService.updateById(employee);
        if (success) {
            employeeCacheService.cacheEmployeeName(employee.getUserId(), employee.getName());
        }
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 删除员工（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = employeeService.removeById(id);
        if (success) {
            employeeCacheService.removeEmployeeName(id);
        }
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @PostConstruct
    @Async
    public void init() {
        List<Employee> employeeList = employeeService.list();
        for (Employee employee : employeeList) {
            employeeCacheService.cacheEmployeeName(employee.getUserId(), employee.getName());
        }
        log.info("初始化员工缓存完成");
    }

    @GetMapping("/exists")
    public Response<Boolean> exists(@RequestParam("userId") Long userId) {
        return Response.ok(employeeService.exists(new LambdaQueryWrapper<Employee>().eq(Employee::getUserId, userId)));
    }

}
