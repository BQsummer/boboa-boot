package com.bqsummer.service.configplus.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface ConfigEle {
    String name() default "";
}
