package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Space between a widget's edge and its children. {@link #value} sets every side, and a non-negative side overrides it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Padding {

    int value() default 0;

    int left() default -1;

    int top() default -1;

    int right() default -1;

    int bottom() default -1;
}
