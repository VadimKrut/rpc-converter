package ru.pathcreator.pyc.rpc.converter.core.model;

import java.lang.reflect.Field;

public record FieldSpec(
        Field field,
        String name,
        Class<?> javaType,
        FieldKind kind,
        Integer fixedLength,
        TypeSpec nestedType
) {
    public boolean variableLength() {
        return kind == FieldKind.STRING || kind == FieldKind.BYTES;
    }

    public boolean fixedLengthCompatible() {
        return !variableLength() && kind != FieldKind.UNSUPPORTED;
    }

    public boolean fixedLengthArray() {
        return kind == FieldKind.FIXED_STRING || kind == FieldKind.FIXED_BYTES;
    }
}