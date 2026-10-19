package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fills this field with the widget of that id from the tree once it's assembled, such as one a layout file made. The id
 * defaults to the field name. Assembling fails if it's missing or of another type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ref {

    String value() default "";
}
