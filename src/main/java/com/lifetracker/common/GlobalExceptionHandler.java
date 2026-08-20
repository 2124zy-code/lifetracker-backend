package com.lifetracker.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[GlobalException] 业务异常拦截: URI={}, Code={}, Message={}", 
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e, HttpServletRequest request) {
        BindingResult bindingResult = (e instanceof MethodArgumentNotValidException) 
                ? ((MethodArgumentNotValidException) e).getBindingResult() 
                : ((BindException) e).getBindingResult();
        
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        StringBuilder sb = new StringBuilder();
        for (FieldError error : fieldErrors) {
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }
        String errorMsg = sb.length() > 0 ? sb.toString() : "入参校验不通过";
        log.warn("[GlobalException] 参数校验失败: URI={}, Error={}", request.getRequestURI(), errorMsg);
        return Result.error(ErrorCodeEnum.PARAM_INVALID.getCode(), errorMsg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("[GlobalException] 数据库唯一索引冲突拦截: URI={}, Error={}", request.getRequestURI(), e.getMessage());
        return Result.error(ErrorCodeEnum.HABIT_ALREADY_LOGGED.getCode(), "数据已存在或操作冲突，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("[GlobalException] 系统内部未捕获异常: URI={}, Message={}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "服务器系统繁忙，请稍后重试");
    }
}
