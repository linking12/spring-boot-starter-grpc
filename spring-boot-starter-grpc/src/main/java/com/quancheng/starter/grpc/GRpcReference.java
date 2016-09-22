package com.quancheng.starter.grpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface GRpcReference {

    String serviceName() default "";

    String version() default "";

    String group() default "";

    // blocking async future
    String callType() default "future";

}
