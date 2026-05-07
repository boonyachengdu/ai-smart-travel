package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.component.JwtComponent;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.vo.UserLoginVO;
import com.boonya.business.trip.common.utils.PasswordEncoderUtil;
import com.boonya.business.trip.user.mapper.UserMapper;
import com.boonya.business.trip.user.service.EmployeeService;
import com.boonya.business.trip.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private EmployeeService employeeService;

    @Autowired
    private JwtComponent jwtComponent;

    @Override
    public User findByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));
    }

    @Override
    public Response<UserLoginVO> login(String username, String password) {
        User user = this.findByUsername(username);
        if (user == null) {
            return Response.error("用户名或密码错误");
        }
        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            return null;
        }

        Employee employee = employeeService.getActiveEmployeeByUserId(user.getId());
        if (employee == null) {
            return Response.error("用户未设置员工信息");
        }

        String token = jwtComponent.generateToken(user.getUsername(), user.getId(), employee.getCompanyId(), employee.getId(), employee.getDepartmentId(), user.getRoles());
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUser(user);
        vo.setEmployee(employee);

        log.info("用户登录成功：userId={}, username={}", user.getId(), user.getUsername());
        return Response.ok(vo);
    }

    @Override
    public User register(String username, String password, String email, String phone, String roles) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        User existingUser = this.findByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoderUtil.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoles(StringUtils.hasText(roles) ? roles : "USER");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        this.save(user);
        log.info("用户注册成功：userId={}, username={}", user.getId(), user.getUsername());

//        try {
//            Employee employee = new Employee();
//            employee.setUserId(user.getId());
//            employeeService.save(employee);
//            log.info("员工信息创建成功：userId={}, employeeId={}", user.getId(), employee.getId());
//        } catch (Exception e) {
//            log.error("员工信息创建失败：userId={}, error={}", user.getId(), e.getMessage(), e);
//        }
        return user;
    }

    @Override
    public Long countUsers() {
        UserHolder userHolder = UserHolder.get();
        if (userHolder.isSuperAdmin()) {
            return userMapper.count();
        }
        return userMapper.countByCompanyId(userHolder.getCompanyId());
    }
}
