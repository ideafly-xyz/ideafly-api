package com.ideafly.aop.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE}) // 可以注解方法和类
@Retention(RetentionPolicy.RUNTIME) // 运行时有效
public @interface NoAuth {
    boolean required() default true; // 默认需要验证，false 表示不需要验证
}