package xyz.volcanobay.empiric.core.ui.annotation;

import xyz.volcanobay.empiric.core.ui.layout.Align;
import xyz.volcanobay.empiric.core.ui.layout.Direction;
import xyz.volcanobay.empiric.core.ui.layout.Justify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lays the widget's children out in a row or column instead of stacking them by anchor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Flex {

    Direction value() default Direction.COLUMN;

    int gap() default 0;

    Align align() default Align.START;

    Justify justify() default Justify.START;
}
