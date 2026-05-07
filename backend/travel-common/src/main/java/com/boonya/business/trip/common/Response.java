package com.boonya.business.trip.common;

import lombok.Data;

@Data
public class Response<T> {
    private boolean success;
    private T data;
    private String message;
    private int code;

    public static <T> Response<T> ok(T data) {
        Response<T> r = new Response<>();
        r.setSuccess(true);
        r.setCode(200);
        r.setData(data);
        return r;
    }

    public static <T> Response<T> ok() {
        return ok(null);
    }

    public static <T> Response<T> error(String msg) {
        Response<T> r = new Response<>();
        r.setSuccess(false);
        r.setCode(500);
        r.setMessage(msg);
        return r;
    }

    // ... 可加更多构造方法
}