package hdc.rjxy.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAdminOp {
    String value() default ""; // 操作描述
}