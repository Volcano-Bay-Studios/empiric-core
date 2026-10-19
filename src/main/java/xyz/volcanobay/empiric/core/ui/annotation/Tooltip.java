package xyz.volcanobay.empiric.core.ui.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shows a synced {@code @Data} field as a row of its module's floating tooltip. Read only by default; {@link #edit()}
 * turns the row into a control for a client mutable field: a switch for booleans, a cycle for enums, a text field for
 * strings and a slider for numbers given a {@link #min()} and {@link #max()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Tooltip {

    /**
     * The row's label. Empty uses the field's name.
     */
    String value() default "";

    /**
     * A {@link String#format} pattern for the value, such as {@code "%.1f Hz"}. Empty shows it as is.
     */
    String format() default "";

    boolean edit() default false;

    double min() default Double.NaN;

    double max() default Double.NaN;

    double step() default 0;

    /**
     * Rows are sorted by this, then by field order.
     */
    int order() default 0;
}
