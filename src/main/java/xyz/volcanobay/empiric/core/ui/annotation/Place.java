package xyz.volcanobay.empiric.core.ui.annotation;

import xyz.volcanobay.empiric.core.ui.layout.Anchor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Where a widget sits inside an anchor layout, plus a pixel offset. On a root widget's class, it places the widget on the
 * screen or surface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Place {

    Anchor value() default Anchor.TOP_LEFT;

    int x() default 0;

    int y() default 0;
}
