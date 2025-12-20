package com.g07.common.exception;

import com.g07.common.core.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    // 捕获业务异常
    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    // 捕获权限不足异常
    @ExceptionHandler(AccessDeniedException.class)
    public R<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: {} {}", request.getMethod(), request.getRequestURI());
        return R.forbidden("权限不足，禁止访问该资源");
    }

    // 捕获系统异常
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}', 发生系统异常.", request.getRequestURI(), e);
        return R.fail("系统繁忙，请稍后重试: " + e.getMessage());
    }
}