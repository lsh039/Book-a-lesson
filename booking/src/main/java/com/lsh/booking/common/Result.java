package com.lsh.booking.common; // 建议放在 common 包下

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 状态码：200成功，500失败
    private String msg;   // 提示信息
    private T data;       // 具体的数据（泛型，可以是User，也可以是List<Course>）

    // 成功的静态方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败的静态方法
    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;

    }
}