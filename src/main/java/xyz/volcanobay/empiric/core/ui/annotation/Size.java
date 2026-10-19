package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A widget's preferred size in pixels, or {@link #FIT} to its content, or {@link #FILL} the free space by {@link #weight}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Size {

    int FIT = -1;
    int FILL = -2;

    int width() default FIT;

    int height() default FIT;

    float weight() default 1;
}
