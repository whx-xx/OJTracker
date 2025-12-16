package hdc.rjxy.cf;

public class CfClientException extends RuntimeException {

    private final String code;

    public CfClientException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CfClientException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
