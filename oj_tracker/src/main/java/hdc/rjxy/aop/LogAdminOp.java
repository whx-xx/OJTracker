package hdc.rjxy.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAdminOp {
    String opType(); // 操作类型，如 "RESET_PWD", "BAN_USER"
}