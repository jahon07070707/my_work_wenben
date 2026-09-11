package com.textanalysis.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public Result<Void> handleMlUnavailable(ResourceAccessException e) {
        log.error("ML service unavailable: {}", e.getMessage());
        return Result.fail("Python ML 服务未启动，请先运行 python/start.bat（端口 8000）");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Server error", e);
        String msg = e.getMessage() != null ? e.getMessage() : "服务器内部错误";
        return Result.fail(msg);
    }
}
