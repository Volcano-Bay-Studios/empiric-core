package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a widget's value, or a {@code Property} field, to a synced {@code @Data} field of the source module, or of the
 * first parent module that has it. A widget field with this annotation is also added as a child. The binding is
 * writable only if the module field is {@code clientMutable}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Bind {

    String value();
}
