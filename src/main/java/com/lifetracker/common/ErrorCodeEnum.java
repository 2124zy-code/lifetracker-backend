package com.lifetracker.common;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    SUCCESS(200, "操作成功"),
    
    // 参数类错误 400~499
    PARAM_INVALID(400, "请求参数校验不通过"),
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    NOT_FOUND(404, "请求资源未找到"),
    
    // 认证与用户业务 1000~1999
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_INVALID(1004, "Token无效或已失效"),
    
    // 习惯与打卡业务 2000~2999
    HABIT_NOT_FOUND(2001, "习惯不存在或已删除"),
    HABIT_ALREADY_LOGGED(2002, "今日该习惯已打卡"),
    HABIT_LOG_DATE_INVALID(2003, "打卡日期超出合理范围"),
    
    // 时间块业务 3000~3999
    TIMEBLOCK_INDEX_INVALID(3001, "时间块索引超出有效范围(0~47)"),
    TIMEBLOCK_CATEGORY_INVALID(3002, "时间块分类无效"),
    
    // AI 复盘业务 4000~4999
    AI_REVIEW_FAILED(4001, "AI精力复盘生成失败"),
    AI_SERVICE_TIMEOUT(4002, "AI诊断服务超时，已降级为规则引擎"),
    
    // 系统通用错误 5000~5999
    SYSTEM_ERROR(5000, "系统繁忙，请稍后再试"),
    DATABASE_OPERATION_FAILED(5001, "数据库操作异常");

    private final Integer code;
    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
