package hdc.rjxy.common.exception;

/**
 * 自定义权限不足异常 (403)
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}