package com.boonya.business.trip.common.exception;

import com.boonya.business.trip.common.Response;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        // 可加日志：log.error("全局异常", e);
        return Response.error("服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Response.error("参数错误: " + e.getMessage());
    }

    // 可加自定义业务异常类
    // @ExceptionHandler(BusinessException.class)
    // ...
}
