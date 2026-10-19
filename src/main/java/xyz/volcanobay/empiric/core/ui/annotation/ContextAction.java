package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds this no-argument method to the right click menu of the child with id {@link #target}, or of the widget itself
 * when the target is empty. Entries appear in declaration order.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ContextAction {

    String value();

    String target() default "";
}
