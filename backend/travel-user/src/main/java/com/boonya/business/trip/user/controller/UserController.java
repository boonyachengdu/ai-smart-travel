package com.boonya.business.trip.user.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Company;
import com.boonya.business.trip.common.entity.Department;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.request.UserQueryRequest;
import com.boonya.business.trip.common.utils.DefaultValue;
import com.boonya.business.trip.common.utils.PasswordEncoderUtil;
import com.boonya.business.trip.user.service.CompanyService;
import com.boonya.business.trip.user.service.UserCacheService;
import com.boonya.business.trip.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CompanyService companyService;
    private final UserService userService;
    private final UserCacheService userCacheService;
    private final EmployeeController employeeController;
    private final DepartmentController departmentController;

    @PostMapping("/page")
    public Response<Page<User>> page(@RequestBody UserQueryRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>();
        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(User::getCompanyId, userHolder.getCompanyId());
        }
        Page<User> page = userService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
                        .like(StringUtils.isNotBlank(request.getUsername()), User::getUsername, request.getUsername())
                        .like(StringUtils.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
                        .orderByDesc(User::getUpdateTime)
        );
        return Response.ok(page);
    }

    @PostMapping("/save")
    public Response<Boolean> save(@RequestBody User user) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            user.setCompanyId(userHolder.getCompanyId());
        }

        // 密码加密（实际项目用 BCryptPasswordEncoder）
        user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));
        boolean success = userService.save(user);
        if (success) {
            userCacheService.cacheUsername(user.getId(), user.getUsername());
            createUserEmployee(user);
        }
        return Response.ok(success);
    }

    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody User user) {
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            user.setCompanyId(userHolder.getCompanyId());
        }
        user.setId(id);
        // 密码不为空才更新
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }
        user.setUpdateTime(LocalDateTime.now());
        boolean success = userService.updateById(user);
        if (success) {
            userCacheService.cacheUsername(user.getId(), user.getUsername());
            Response<Boolean> employeeResponse = employeeController.exists(user.getId());
            if (!employeeResponse.getData()) {
                createUserEmployee(user);
            }
        }
        return Response.ok(success);
    }

    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = userService.removeById(id);
        if (success) {
            userCacheService.removeUserCache(id);
        }
        return Response.ok(success);
    }

    @GetMapping("/{id}")
    public Response<User> getUser(@PathVariable("id") Long id) {
        return Response.ok(userService.getById(id));
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @PostConstruct
    @Async
    public void init() {
        // 缓存用户名
        List<User> userList = userService.list();
        if (!CollectionUtils.isEmpty(userList)) {
            userList.forEach(user -> userCacheService.cacheUsername(user.getId(), user.getUsername()));
        }
        log.info("用户缓存初始化完成");
    }

    @GetMapping("/countUsers")
    public Response<Long> countUsers() {
        return Response.ok(userService.countUsers());
    }

    @GetMapping("/exists")
    public Response<Boolean> exists(@RequestParam("username") String username) {
        return Response.ok(userService.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username)));
    }

    @GetMapping("/userInfo")
    public Response<UserHolder> userInfo() {
        UserHolder userHolder = UserHolder.get();
        return Response.ok(userHolder);
    }

    private void createUserEmployee(User user) {
        Response<List<Department>> departmentResponse = departmentController.list();
        // 创建员工账号
        Employee employee = new Employee();
        employee.setCompanyId(user.getCompanyId());
        if (ObjectUtils.isEmpty(departmentResponse.getData())){
            Company company = companyService.getById(user.getCompanyId());
            Department department = createDepartment(company);
            employee.setDepartmentId(department.getId());
        } else {
            employee.setDepartmentId(departmentResponse.getData().get(0).getId());
        }
        employee.setUserId(user.getId());
        employee.setName(DefaultValue.DEFAULT_NAME);
        employee.setPosition(DefaultValue.DEFAULT_POSITION);
        employee.setEmployeeNo(DefaultValue.DEFAULT_EMPLOYEE_NO);
        employee.setIsPrimary(true);
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        employeeController.save(employee);
    }

    private Department createDepartment(Company company) {
        Department department = new Department();
        department.setName("总经办");
        department.setCompanyId(company.getId());
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        department.setDeleted(0);
        departmentController.save(department);
        return department;
    }
}
