package com.boonya.business.trip.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.RoleType;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Department;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.request.CompanyQueryRequest;
import com.boonya.business.trip.user.service.CompanyCacheService;
import com.boonya.business.trip.user.service.CompanyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyCacheService companyCacheService;
    private final UserController userController;
    private final DepartmentController departmentController;

    /**
     * 分页查询企业列表
     */
    @PostMapping("/page")
    public Response<Page<Company>> page(@RequestBody CompanyQueryRequest request) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Company::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Company::getId, userHolder.getCompanyId());
        }

        if (StringUtils.hasText(request.getName())) {
            wrapper.like(Company::getName, request.getName());
        }
        if (StringUtils.hasText(request.getUserName())) {
            wrapper.like(Company::getUserName, request.getUserName());
        }

        wrapper.orderByDesc(Company::getUpdateTime);

        Page<Company> page = companyService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询单个企业
     */
    @GetMapping("/{id}")
    public Response<Company> getById(@PathVariable("id") Long id) {
        Company company = companyService.getById(id);
        return company != null ? Response.ok(company) : Response.error("企业不存在");
    }

    /**
     * 新增企业（自动创建企业账号）
     */
    @PostMapping
    public Response<Long> save(@RequestBody Company company) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            return Response.error("无权限创建企业");
        }

        // 默认密码或抛异常，根据业务决定
        if (!StringUtils.hasText(company.getPassword())) {
            company.setPassword("123456");
        }


        // 检查用户名
        Response<Boolean> userResponse = userController.exists(company.getUserName());
        if (userResponse.getData()) {
            return Response.error("用户名已存在");
        }

        // 设置默认值
        if (company.getAutoApprove() == null) {
            company.setAutoApprove(false);
        }
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());

        boolean success = companyService.save(company);
        if (success) {
            companyCacheService.cacheCompanyName(company.getId(), company.getName());

            createDepartment(company);

            createAdminAccount(company);
        }
        return success ? Response.ok(company.getId()) : Response.error("新增失败");
    }

    /**
     * 修改企业
     */
    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Company company) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            return Response.error("无权限修改企业");
        }

        company.setId(id);
        // 账号和密码不允许修改
        company.setUserName(null);
        company.setPassword(null);
        company.setUpdateTime(LocalDateTime.now());

        boolean success = companyService.updateById(company);
        if (success) {
            companyCacheService.cacheCompanyName(company.getId(), company.getName());

            Response<List<Department>> departmentResponse = departmentController.list();
            if (departmentResponse.getData().isEmpty()) {
                createDepartment(company);
            }

            company = companyService.getById(id);
            Response<Boolean> userResponse = userController.exists(company.getUserName());
            if (!userResponse.getData()) {
                createAdminAccount(company);
            }
        }
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 删除企业（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            return Response.error("无权限删除企业");
        }

        boolean success = companyService.removeById(id);
        if (success) {
            companyCacheService.removeCompanyCache(id);
        }
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    /**
     * 启用/禁用自动审批
     */
    @PutMapping("/{id}/toggle-auto-approve")
    public Response<Boolean> toggleAutoApprove(@PathVariable("id") Long id, @RequestParam Boolean autoApprove) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            return Response.error("无权限修改是否自动审批");
        }
        Company company = new Company();
        company.setId(id);
        company.setAutoApprove(autoApprove);
        boolean success = companyService.updateById(company);
        return success ? Response.ok(true) : Response.error("操作失败");
    }

    /**
     * 获取公司列表（用于下拉选择）
     */
    @GetMapping("/list")
    public Response<List<Map<String, Object>>> list() {
        UserHolder userHolder = UserHolder.get();

        List<Map<String, Object>> companies = new ArrayList<>();
        if (userHolder.isSuperAdmin()) {
            List<Company> companyList = companyService.list();
            for (Company company : companyList) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", company.getId());
                map.put("name", company.getName());
                companies.add(map);
                companyCacheService.cacheCompanyName(company.getId(), company.getName());
            }
        } else {
            Company company = companyService.getById(userHolder.getCompanyId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", company.getId());
            map.put("name", company.getName());
            companies.add(map);
            companyCacheService.cacheCompanyName(company.getId(), company.getName());
        }

        return Response.ok(companies);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @PostConstruct
    @Async
    public void init() {
        List<Company> companyList = companyService.list();
        for (Company company : companyList) {
            companyCacheService.cacheCompanyName(company.getId(), company.getName());
        }
        log.info("初始化企业缓存完成");
    }

    private void createAdminAccount(Company company) {
        // 创建管理账号
        User user = new User();
        user.setUsername(company.getUserName());
        user.setPassword(company.getPassword());
        user.setDeleted(0);
        user.setRoles(RoleType.ROLE_ADMIN.name());
        // 企业创建员工必要字段
        user.setCompanyId(company.getId());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userController.save(user);
    }

    private void createDepartment(Company company) {
        Department department = new Department();
        department.setName("总经办");
        department.setCompanyId(company.getId());
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        department.setDeleted(0);
        departmentController.save(department);
    }

}
