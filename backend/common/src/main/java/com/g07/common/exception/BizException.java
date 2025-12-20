package com.g07.common.exception;

import lombok.Getter;

// 业务异常
@Getter
public class BizException extends RuntimeException {
    private Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}