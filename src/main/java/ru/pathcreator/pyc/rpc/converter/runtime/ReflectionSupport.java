package ru.pathcreator.pyc.rpc.converter.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Reflection helpers used by generated codecs to instantiate DTOs and access fields.
 */
public final class ReflectionSupport {

    private ReflectionSupport() {
    }

    /**
     * Resolves and opens a no-args constructor.
     *
     * @param type target DTO type
     * @param <T>  DTO type
     * @return accessible constructor
     */
    public static <T> Constructor<T> noArgsConstructor(final Class<T> type) {
        try {
            final Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("No accessible no-args constructor for " + type.getName(), e);
        }
    }

    /**
     * Instantiates a DTO through a previously resolved constructor.
     *
     * @param constructor accessible no-args constructor
     * @param <T>         DTO type
     * @return new DTO instance
     */
    public static <T> T instantiate(final Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate " + constructor.getDeclaringClass().getName(), e);
        }
    }

    /**
     * Resolves a {@link VarHandle} for a declared field.
     *
     * @param owner     declaring class
     * @param fieldName field name
     * @param fieldType field type
     * @return resolved var handle
     */
    public static VarHandle varHandle(final Class<?> owner, final String fieldName, final Class<?> fieldType) {
        try {
            final Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
            return lookup.findVarHandle(owner, fieldName, fieldType);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot resolve field " + owner.getName() + "." + fieldName, e);
        }
    }
}