package com.boonya.business.trip.common.aop;

import com.boonya.business.trip.common.context.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
@Component
@Slf4j
public class UserHolderAspect {

    // 定义切点：所有 Controller 方法（可根据需要调整）
    @Pointcut("execution(* com.boonya.business.trip..controller.*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object setUserContext(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 从当前请求中获取 HttpServletRequest
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 从请求头中提取网关传递的用户信息
                String username = request.getHeader("X-User-Name");
                String userId = request.getHeader("X-User-Id");
                String companyId = request.getHeader("X-Company-Id");
                String employeeId = request.getHeader("X-Employee-Id");
                String deptId = request.getHeader("X-Dept-Id");
                String roles = request.getHeader("X-Roles");

                if (StringUtils.hasText(userId)) {
                    UserHolder context = new UserHolder();
                    context.setUsername(username);
                    context.setUserId(Long.valueOf(userId));
                    context.setCompanyId(StringUtils.hasText(companyId) ? Long.valueOf(companyId) : null);
                    context.setEmployeeId(StringUtils.hasText(employeeId) ? Long.valueOf(employeeId) : null);
                    context.setDeptId(StringUtils.hasText(deptId) ? Long.valueOf(deptId) : null);
                    context.setRoles(roles);
                    UserHolder.set(context);
                }
            }

            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 清理用户信息 TODO 长连接需考虑token更新，前端需要定期刷新token
            UserHolder.clear();
        }
    }
}
