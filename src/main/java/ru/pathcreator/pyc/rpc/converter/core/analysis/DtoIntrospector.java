package ru.pathcreator.pyc.rpc.converter.core.analysis;

import ru.pathcreator.pyc.rpc.converter.annotations.RpcFieldOrder;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcFixedLength;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.model.FieldKind;
import ru.pathcreator.pyc.rpc.converter.core.model.FieldSpec;
import ru.pathcreator.pyc.rpc.converter.core.model.InstantiationStyle;
import ru.pathcreator.pyc.rpc.converter.core.model.TypeSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.*;

public final class DtoIntrospector {

    public TypeSpec inspectRoot(final Class<?> rootType) {
        return inspect(rootType, true, new IdentityHashMap<>());
    }

    private TypeSpec inspect(final Class<?> type, final boolean root, final IdentityHashMap<Class<?>, Boolean> stack) {
        if (stack.containsKey(type)) {
            throw new IllegalStateException("Recursive DTO graph is not supported for SBE generation: " + type.getName());
        }
        stack.put(type, Boolean.TRUE);
        final List<Field> fields = declaredInstanceFields(type);
        final List<FieldSpec> specs = new ArrayList<>(fields.size());
        for (final Field field : fields) {
            specs.add(classifyField(field, root, stack));
        }
        stack.remove(type);
        final RpcSbe annotation = type.getAnnotation(RpcSbe.class);
        final String schemaName = annotation != null && !annotation.schemaName().isBlank()
                ? annotation.schemaName()
                : type.getSimpleName();
        return new TypeSpec(type, schemaName, instantiationStyle(type), specs);
    }

    private FieldSpec classifyField(final Field field, final boolean root, final IdentityHashMap<Class<?>, Boolean> stack) {
        final Class<?> type = field.getType();
        final RpcFixedLength fixedLength = field.getAnnotation(RpcFixedLength.class);
        final Integer fixed = fixedLength != null ? fixedLength.value() : null;
        if (type.isPrimitive()) {
            return new FieldSpec(field, field.getName(), type, primitiveKind(type), fixed, null);
        }
        if (isBoxedPrimitive(type)) {
            return new FieldSpec(field, field.getName(), type, FieldKind.BOXED_PRIMITIVE, fixed, null);
        }
        if (type == Boolean.class) {
            return new FieldSpec(field, field.getName(), type, FieldKind.BOXED_BOOLEAN, fixed, null);
        }
        if (type == Character.class) {
            return new FieldSpec(field, field.getName(), type, FieldKind.BOXED_CHAR, fixed, null);
        }
        if (type.isEnum()) {
            return new FieldSpec(field, field.getName(), type, FieldKind.ENUM, fixed, null);
        }
        if (type == String.class) {
            return new FieldSpec(field, field.getName(), type, fixed != null ? FieldKind.FIXED_STRING : FieldKind.STRING, fixed, null);
        }
        if (type == byte[].class) {
            return new FieldSpec(field, field.getName(), type, fixed != null ? FieldKind.FIXED_BYTES : FieldKind.BYTES, fixed, null);
        }
        if (type.isArray()
            || Collection.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type)
            || Set.class.isAssignableFrom(type)
            || Optional.class.isAssignableFrom(type)
            || BigDecimal.class.isAssignableFrom(type)
            || BigInteger.class.isAssignableFrom(type)
            || Temporal.class.isAssignableFrom(type)
            || type == Object.class
            || type.isInterface()
            || Modifier.isAbstract(type.getModifiers())
            || type.isAnonymousClass()
            || type.isLocalClass()) {
            return new FieldSpec(field, field.getName(), type, FieldKind.UNSUPPORTED, fixed, null);
        }
        final TypeSpec nestedType = inspect(type, false, stack);
        return new FieldSpec(field, field.getName(), type, FieldKind.NESTED_FIXED, fixed, nestedType);
    }

    private InstantiationStyle instantiationStyle(final Class<?> type) {
        if (type.isRecord()) {
            return InstantiationStyle.RECORD;
        }
        try {
            final Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return InstantiationStyle.NO_ARGS_CONSTRUCTOR;
        } catch (final NoSuchMethodException e) {
            return InstantiationStyle.UNSUPPORTED_FOR_SBE;
        }
    }

    private FieldKind primitiveKind(final Class<?> type) {
        if (type == boolean.class) {
            return FieldKind.BOOLEAN;
        }
        if (type == char.class) {
            return FieldKind.CHAR;
        }
        return FieldKind.PRIMITIVE;
    }

    private boolean isBoxedPrimitive(final Class<?> type) {
        return type == Byte.class || type == Short.class || type == Integer.class || type == Long.class
               || type == Float.class || type == Double.class;
    }

    private List<Field> declaredInstanceFields(final Class<?> type) {
        final Map<String, Integer> declarationOrder = new LinkedHashMap<>();
        final List<Field> fields = new ArrayList<>();
        final Field[] declaredFields = type.getDeclaredFields();
        for (int i = 0; i < declaredFields.length; i++) {
            final Field field = declaredFields[i];
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
            declarationOrder.put(field.getName(), i);
        }
        fields.sort(Comparator
                .comparingInt((Field field) -> {
                    RpcFieldOrder order = field.getAnnotation(RpcFieldOrder.class);
                    return order != null ? order.value() : Integer.MAX_VALUE;
                })
                .thenComparingInt(field -> declarationOrder.getOrDefault(field.getName(), Integer.MAX_VALUE)));
        return fields;
    }
}