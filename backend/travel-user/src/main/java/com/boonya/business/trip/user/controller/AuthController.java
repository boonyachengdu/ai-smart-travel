package com.boonya.business.trip.user.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.component.JwtComponent;
import com.boonya.business.trip.common.component.TokenManager;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Employee;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.common.models.user.request.LoginRequest;
import com.boonya.business.trip.common.models.user.request.RegisterRequest;
import com.boonya.business.trip.common.models.user.vo.UserLoginVO;
import com.boonya.business.trip.user.service.EmployeeService;
import com.boonya.business.trip.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtComponent jwtComponent;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenManager tokenManager;

    @PostMapping("/register")
    public Response<User> register(@RequestBody RegisterRequest request) {
        try {
            // 1. 验证邮箱验证码（如果提供了邮箱）
            if (StringUtils.hasText(request.getEmail())) {
                String emailCaptchaKey = "register:captcha:email:" + request.getEmail();
                Object storedCaptcha = redisTemplate.opsForValue().get(emailCaptchaKey);

                if (storedCaptcha == null) {
                    return Response.error("邮箱验证码已过期，请重新获取");
                }

                if (!storedCaptcha.toString().equals(request.getCaptcha())) {
                    return Response.error("邮箱验证码错误");
                }

                // 验证成功后删除验证码，防止重复使用
                redisTemplate.delete(emailCaptchaKey);
                log.info("邮箱验证码验证通过：email={}", request.getEmail());
            }
            // 2. 验证手机验证码（如果提供了手机号）
            else if (StringUtils.hasText(request.getPhone())) {
                String phoneCaptchaKey = "register:captcha:phone:" + request.getPhone();
                Object storedCaptcha = redisTemplate.opsForValue().get(phoneCaptchaKey);

                if (storedCaptcha == null) {
                    return Response.error("手机验证码已过期，请重新获取");
                }

                if (!storedCaptcha.toString().equals(request.getCaptcha())) {
                    return Response.error("手机验证码错误");
                }

                // 验证成功后删除验证码，防止重复使用
                redisTemplate.delete(phoneCaptchaKey);
                log.info("手机验证码验证通过：phone={}", request.getPhone());
            }

            // 3. 如果邮箱和手机号都为空，报错
            if (!StringUtils.hasText(request.getEmail()) && !StringUtils.hasText(request.getPhone())) {
                return Response.error("邮箱和手机号不能同时为空");
            }

            // 4. 验证码验证通过，执行注册
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getRoles()
            );
            log.info("用户注册成功：userId={}, username={}", user.getId(), user.getUsername());
            return Response.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("用户注册失败：{}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return Response.error("注册失败：" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public Response<UserLoginVO> login(@RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/logout")
    public Response<Void> logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                long ttlSeconds = getTokenRemainingValidity(token);
                tokenManager.addToBlacklist(token, ttlSeconds);

                UserHolder userHolder = UserHolder.get();
                Long userId = userHolder != null ? userHolder.getUserId() : null;
                log.debug("用户登出，token 已加入黑名单：userId={}", userId);
            }

            log.info("用户登出成功");
            return Response.ok();
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Response.error("登出失败：" + e.getMessage());
        }
    }

    @PostMapping("/captcha")
    public Response<Map<String, Object>> getCaptcha(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String phone = params.get("phone");

            // 参数校验
            if (!StringUtils.hasText(email) && !StringUtils.hasText(phone)) {
                return Response.error("邮箱和手机号不能同时为空");
            }

            // 生成 6 位数字验证码
            String captcha = String.format("%06d", new Random().nextInt(1000000));

            // 将验证码存储到 Redis，有效期 5 分钟
            String captchaKey = null;
            if (StringUtils.hasText(email)) {
                captchaKey = "register:captcha:email:" + email;
                log.info("生成邮箱验证码：email={}, captcha={}", email, captcha);
                // TODO: 实际项目中需要发送邮件
                // emailService.sendVerificationCode(email, captcha);
            } else if (StringUtils.hasText(phone)) {
                captchaKey = "register:captcha:phone:" + phone;
                log.info("生成手机验证码：phone={}, captcha={}", phone, captcha);
                // TODO: 实际项目中需要发送短信
                // smsService.sendVerificationCode(phone, captcha);
            }

            // 存储到 Redis
            redisTemplate.opsForValue().set(captchaKey, captcha, 5, TimeUnit.MINUTES);

            Map<String, Object> result = new HashMap<>();
            result.put("captcha", captcha);
            result.put("expireIn", 300); // 5 分钟过期
            result.put("captchaKey", captchaKey);

            log.info("验证码生成成功，key={}", captchaKey);
            return Response.ok(result);

        } catch (Exception e) {
            log.error("生成验证码失败", e);
            return Response.error("验证码生成失败：" + e.getMessage());
        }
    }

    /**
     * 验证验证码（可选独立接口）
     */
    @PostMapping("/verify-captcha")
    public Response<Boolean> verifyCaptcha(@RequestBody Map<String, String> params) {
        try {
            String email = params.get("email");
            String phone = params.get("phone");
            String captcha = params.get("captcha");

            if (!StringUtils.hasText(captcha)) {
                return Response.error("验证码不能为空");
            }

            String captchaKey = null;
            if (StringUtils.hasText(email)) {
                captchaKey = "register:captcha:email:" + email;
            } else if (StringUtils.hasText(phone)) {
                captchaKey = "register:captcha:phone:" + phone;
            }

            if (captchaKey == null) {
                return Response.error("邮箱和手机号不能同时为空");
            }

            Object storedCaptcha = redisTemplate.opsForValue().get(captchaKey);

            if (storedCaptcha == null) {
                return Response.ok(false); // 验证码不存在或已过期
            }

            boolean isValid = storedCaptcha.toString().equals(captcha);

            if (isValid) {
                // 验证成功后删除验证码
                redisTemplate.delete(captchaKey);
                log.info("验证码验证通过：key={}", captchaKey);
            } else {
                log.warn("验证码错误：key={}, expected={}, actual={}", captchaKey, storedCaptcha, captcha);
            }

            return Response.ok(isValid);

        } catch (Exception e) {
            log.error("验证验证码失败", e);
            return Response.error("验证码验证失败：" + e.getMessage());
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private long getTokenRemainingValidity(String token) {
        try {
            var claims = jwtComponent.parseToken(token);
            long exp = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            return Math.max(0, (exp - now) / 1000);
        } catch (Exception e) {
            log.warn("解析 token 过期时间失败，使用默认 24 小时", e);
            return 86400;
        }
    }
}
