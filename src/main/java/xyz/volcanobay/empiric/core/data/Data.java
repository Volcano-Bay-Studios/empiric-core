package xyz.volcanobay.empiric.core.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as serializable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Data {

    String id() default "";

    /**
     * On load, if {@link Encoding#KEYED}, will try to fetch fields from the alias fields if id misses
     */
    String[] aliases() default {};

    boolean optional() default false;

    boolean save() default true;

    boolean sync() default true;

    /**
     * If watch is enabled, the data will automatically be marked as dirty if the value changes.
     * If it is not enabled, you must manually mark the field as dirty.
     */
    boolean watch() default false;

    /**
     * If enabled, the field still syncs from the server to the client, and changes made on the client
     * are also sent back to the server, following the same dirty and watch rules.
     */
    boolean clientMutable() default false;

    Mode mode() default Mode.INHERIT;

    int depth() default -1;

    Class<? extends ModeResolver> modeResolver() default ModeResolver.None.class;

    Class<? extends FieldReader> reader() default FieldReader.None.class;

    ListUpdate listUpdate() default ListUpdate.SET;
}
