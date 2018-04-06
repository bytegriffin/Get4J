package com.bytegriffin.get4j.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ListDetail {

    int thread() default 1;

    String url();//没有default值必须要设置
    
    String method() default "get";

    String probeSelector() default "";

    int probeSleep() default 60;

    String detailSelector() default "";

    String totolPages() default "1";

    String startTime() default "";

    int interval() default 0;

    int sleep() default 0;

    String sleepRange() default "";

    String proxy() default "";

    String userAgent() default "";

    String resourceSelector() default "";

    String downloadDisk() default "default";

    String downloadHdfs() default "";

    boolean javascriptSupport() default false;

    String parser() default "";

    String jdbc() default "";

    String lucene() default "default";

    String hbase() default "";
}
