package com.bqsummer.framework.configplus.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ConfplusRegister.class})
public @interface ConfigplusScan {
    String[] basePackages() default {};
}
