package hdc.rjxy.cf;

import lombok.Getter;

/**
 * Codeforces 客户端专用异常
 * 用于在 Service 层抛出，由 GlobalExceptionHandler 统一捕获处理
 */
@Getter
public class CfClientException extends RuntimeException {

    // 错误代码，例如：CF_TIMEOUT, CF_HTTP_503, HANDLE_MISSING
    private final String code;

    public CfClientException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CfClientException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}