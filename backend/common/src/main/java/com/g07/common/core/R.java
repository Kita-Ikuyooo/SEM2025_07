package com.g07.common.core;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 200:成功, 500:系统错误, 401:未认证, 403:禁止访问
    private Integer code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(500, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> forbidden(String msg) {
        return fail(403, msg != null ? msg : "权限不足，禁止访问");
    }
}