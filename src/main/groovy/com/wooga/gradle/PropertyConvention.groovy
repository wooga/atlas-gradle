package com.wooga.gradle

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.TYPE])
@interface PropertyConvention {
    String[] environmentKeys() default null

    String[] propertyKeys() default null
}

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.TYPE])
@interface StringPropertyConvention {
    String defaultValue() default null

    String[] environmentKeys() default null

    String[] propertyKeys() default null
}
