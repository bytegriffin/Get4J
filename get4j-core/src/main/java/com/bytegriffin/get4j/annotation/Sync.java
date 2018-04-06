package com.bytegriffin.get4j.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sync {

    String protocal() default "rsync";

    int batchCount() default 10;

    int batchTime() default 10;

    String host();//没有default值必须要设置

    int port() default 21;

    String username() default "";

    String password() default "";

    String dir() default "";

    String module() default "";

    boolean isModule() default true;
}
