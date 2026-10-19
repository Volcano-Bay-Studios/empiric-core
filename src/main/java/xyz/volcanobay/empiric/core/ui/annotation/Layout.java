package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Takes this widget's children and styling from a layout file, by id. The file's widgets are added before any
 * {@code @Child} fields, and {@code @OnClick}, {@code @ContextAction} and {@code @Ref} find them by id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Layout {

    String value();
}
