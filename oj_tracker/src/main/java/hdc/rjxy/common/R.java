package hdc.rjxy.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 通用 API 响应封装类
 * 统一后端返回格式：{ "code": 200, "msg": "success", "data": ... }
 */
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 默认失败状态码 */
    public static final int ERROR_CODE = 500;

    private int code;
    private String msg;
    private T data;

    // 私有构造，强制使用静态方法创建
    private R() {}

    // ============================ 成功响应 ============================

    /**
     * 成功 - 无数据
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 成功 - 带数据
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    /**
     * 成功 - 自定义消息和数据
     */
    public static <T> R<T> ok(String msg, T data) {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    // ============================ 失败响应 ============================

    /**
     * 失败 - 使用默认错误码和消息
     */
    public static <T> R<T> fail() {
        return fail(ERROR_CODE, "操作失败");
    }

    /**
     * 失败 - 指定错误消息 (最常用)
     */
    public static <T> R<T> fail(String msg) {
        return fail(ERROR_CODE, msg);
    }

    /**
     * 失败 - 指定错误码和消息 (处理特定业务异常时用)
     */
    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}