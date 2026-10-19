package xyz.volcanobay.empiric.core.ui.bind;

import xyz.volcanobay.empiric.core.data.layout.Accessors;
import xyz.volcanobay.empiric.core.ui.Widget;
import xyz.volcanobay.empiric.core.ui.annotation.Bind;
import xyz.volcanobay.empiric.core.ui.annotation.Child;
import xyz.volcanobay.empiric.core.ui.annotation.Clip;
import xyz.volcanobay.empiric.core.ui.annotation.ContextAction;
import xyz.volcanobay.empiric.core.ui.annotation.Flex;
import xyz.volcanobay.empiric.core.ui.annotation.Hint;
import xyz.volcanobay.empiric.core.ui.annotation.Layer;
import xyz.volcanobay.empiric.core.ui.annotation.Layout;
import xyz.volcanobay.empiric.core.ui.annotation.Margin;
import xyz.volcanobay.empiric.core.ui.annotation.OnChange;
import xyz.volcanobay.empiric.core.ui.annotation.OnClick;
import xyz.volcanobay.empiric.core.ui.annotation.Padding;
import xyz.volcanobay.empiric.core.ui.annotation.Place;
import xyz.volcanobay.empiric.core.ui.annotation.Ref;
import xyz.volcanobay.empiric.core.ui.annotation.Size;
import xyz.volcanobay.empiric.core.ui.annotation.Theme;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The UI annotations of one widget class, read once with reflection and turned into generated accessors.
 */
final class UiClass {

    private static final ClassValue<UiClass> CACHE = new ClassValue<>() {
        @Override
        protected UiClass computeValue(Class<?> type) {
            return new UiClass(type);
        }
    };
    private static final List<Class<? extends Annotation>> CHILD_ONLY = List.of(Place.class, Margin.class, Layer.class, Hint.class);

    final List<ChildField> children = new ArrayList<>();
    final List<Handler> changes = new ArrayList<>();
    final List<Handler> clicks = new ArrayList<>();
    final List<Handler> contextActions = new ArrayList<>();
    final List<RefField> refs = new ArrayList<>();
    final Size size;
    final Padding padding;
    final Flex flex;
    final Place place;
    final boolean clip;
    final String theme;
    final String layout;

    record ChildField(String id, String describe, boolean property, Accessors.ObjectGetter getter, Accessors.ObjectSetter setter, Bind bind, Size size,
                      Place place, Margin margin, Padding padding, Layer layer, Flex flex, boolean clip, Hint hint) {
    }

    record RefField(String id, String describe, Class<?> type, Accessors.ObjectSetter setter) {
    }

    record Handler(String target, String describe, UiBinder.Invoker invoker, UiBinder.ValueInvoker valueInvoker, String label) {
    }

    private UiClass(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Widget.class && current != Object.class; current = current.getSuperclass()) {
            hierarchy.addFirst(current);
        }
        for (Class<?> declaring : hierarchy) {
            for (Field field : declaring.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    readField(field);
                }
            }
            for (Method method : declaring.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    readMethod(method);
                }
            }
        }
        this.size = find(type, Size.class);
        this.padding = find(type, Padding.class);
        this.flex = find(type, Flex.class);
        this.place = find(type, Place.class);
        this.clip = find(type, Clip.class) != null;
        Theme theme = find(type, Theme.class);
        this.theme = theme == null ? null : theme.value();
        Layout layout = find(type, Layout.class);
        this.layout = layout == null ? null : layout.value();
    }

    static UiClass of(Class<?> type) {
        return CACHE.get(type);
    }

    boolean empty() {
        return children.isEmpty() && changes.isEmpty() && clicks.isEmpty() && contextActions.isEmpty() && refs.isEmpty() && layout == null && size == null && padding == null && flex == null && place == null && !clip;
    }

    private void readField(Field field) {
        Child child = field.getAnnotation(Child.class);
        Bind bind = field.getAnnotation(Bind.class);
        Ref ref = field.getAnnotation(Ref.class);
        String describe = field.getDeclaringClass().getName() + "." + field.getName();
        if (ref != null) {
            if (child != null || bind != null) {
                throw new UiException(describe + " is a @Ref, so it can't also be a @Child or @Bind");
            }
            if (!Widget.class.isAssignableFrom(field.getType()) || Modifier.isFinal(field.getModifiers())) {
                throw new UiException(describe + " is a @Ref, so it must be a non-final Widget field");
            }
            try {
                Accessors.ObjectSetter setter = Accessors.bind(Accessors.ObjectSetter.class, lookup(field).unreflectSetter(field));
                refs.add(new RefField(ref.value().isEmpty() ? field.getName() : ref.value(), describe, field.getType(), setter));
            } catch (IllegalAccessException e) {
                throw new UiException("Can't access " + describe, e);
            }
            return;
        }
        if (child == null && bind == null) {
            for (Class<? extends Annotation> annotation : CHILD_ONLY) {
                if (field.isAnnotationPresent(annotation)) {
                    throw new UiException(describe + " has @" + annotation.getSimpleName() + " but isn't a @Child");
                }
            }
            return;
        }
        boolean property = Property.class.isAssignableFrom(field.getType());
        if (property) {
            if (bind == null || child != null) {
                throw new UiException(describe + " is a Property, so it takes @Bind and nothing else");
            }
            if (Modifier.isFinal(field.getModifiers())) {
                throw new UiException(describe + " is final, so @Bind can't set it");
            }
        } else if (!Widget.class.isAssignableFrom(field.getType())) {
            throw new UiException(describe + " is a @Child or @Bind but isn't a Widget or Property");
        }
        MethodHandles.Lookup lookup = lookup(field);
        try {
            Accessors.ObjectGetter getter = Accessors.bind(Accessors.ObjectGetter.class, lookup.unreflectGetter(field));
            Accessors.ObjectSetter setter = property ? Accessors.bind(Accessors.ObjectSetter.class, lookup.unreflectSetter(field)) : null;
            String id = child != null && !child.id().isEmpty() ? child.id() : field.getName();
            children.add(new ChildField(id, describe, property, getter, setter, bind, field.getAnnotation(Size.class), field.getAnnotation(Place.class),
                    field.getAnnotation(Margin.class), field.getAnnotation(Padding.class), field.getAnnotation(Layer.class), field.getAnnotation(Flex.class),
                    field.isAnnotationPresent(Clip.class), field.getAnnotation(Hint.class)));
        } catch (IllegalAccessException e) {
            throw new UiException("Can't access " + describe, e);
        }
    }

    private void readMethod(Method method) {
        OnChange change = method.getAnnotation(OnChange.class);
        OnClick click = method.getAnnotation(OnClick.class);
        ContextAction action = method.getAnnotation(ContextAction.class);
        if (change == null && click == null && action == null) {
            return;
        }
        String describe = method.getDeclaringClass().getName() + "." + method.getName();
        int parameters = method.getParameterCount();
        if ((click != null || action != null) && parameters != 0) {
            throw new UiException(describe + " is an @OnClick or @ContextAction, so it can't take arguments");
        }
        if (change != null && parameters > 1) {
            throw new UiException(describe + " is an @OnChange, so it takes no arguments or the new value");
        }
        MethodHandle handle;
        try {
            handle = lookup(method).unreflect(method);
        } catch (IllegalAccessException e) {
            throw new UiException("Can't access " + describe, e);
        }
        UiBinder.Invoker invoker = parameters == 0 ? Accessors.bind(UiBinder.Invoker.class, handle) : null;
        if (click != null) {
            clicks.add(new Handler(click.value(), describe, invoker, null, null));
        }
        if (action != null) {
            contextActions.add(new Handler(action.target(), describe, invoker, null, action.value()));
        }
        if (change != null) {
            UiBinder.ValueInvoker valueInvoker = parameters == 1 ? Accessors.bind(UiBinder.ValueInvoker.class, handle) : null;
            changes.add(new Handler(change.value(), describe, invoker, valueInvoker, null));
        }
    }

    private static MethodHandles.Lookup lookup(Member member) {
        try {
            return MethodHandles.privateLookupIn(member.getDeclaringClass(), MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            ((AccessibleObject) member).setAccessible(true);
            return MethodHandles.lookup();
        }
    }

    private static <A extends Annotation> A find(Class<?> type, Class<A> annotation) {
        for (Class<?> current = type; current != null && current != Widget.class; current = current.getSuperclass()) {
            A found = current.getAnnotation(annotation);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
