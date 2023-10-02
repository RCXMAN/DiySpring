package com.practice.diy.annotation;

import java.lang.annotation.*;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configuration {
    String value() default "";
}
