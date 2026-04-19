package ru.pathcreator.pyc.rpc.converter.core.model;

import java.lang.reflect.Field;

/**
 * Normalized description of one DTO field.
 *
 * @param field       reflected Java field
 * @param name        logical field name used in generated artifacts
 * @param javaType    Java field type
 * @param kind        normalized field category
 * @param fixedLength optional fixed-length hint from {@code @RpcFixedLength}
 * @param nestedType  nested type model for inline composites
 */
public record FieldSpec(
        Field field,
        String name,
        Class<?> javaType,
        FieldKind kind,
        Integer fixedLength,
        TypeSpec nestedType
) {
    /**
     * Returns whether the field is encoded outside the fixed-size block.
     */
    public boolean variableLength() {
        return kind == FieldKind.STRING || kind == FieldKind.BYTES;
    }

    /**
     * Returns whether the field can participate in a fixed-size nested
     * composite.
     */
    public boolean fixedLengthCompatible() {
        return !variableLength() && kind != FieldKind.UNSUPPORTED;
    }

    /**
     * Returns whether the field uses a fixed-size array representation.
     */
    public boolean fixedLengthArray() {
        return kind == FieldKind.FIXED_STRING || kind == FieldKind.FIXED_BYTES;
    }
}