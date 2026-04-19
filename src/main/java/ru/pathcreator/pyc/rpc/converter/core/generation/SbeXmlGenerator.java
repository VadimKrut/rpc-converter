package ru.pathcreator.pyc.rpc.converter.core.generation;

import ru.pathcreator.pyc.rpc.converter.core.model.FieldKind;
import ru.pathcreator.pyc.rpc.converter.core.model.FieldSpec;
import ru.pathcreator.pyc.rpc.converter.core.model.TypeSpec;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders an SBE XML schema for one analyzed DTO root.
 *
 * <p>The generated schema mirrors the subset supported by the internal planner:
 * fixed-size primitive fields are emitted as normal SBE fields, top-level
 * variable-length values are emitted as {@code <data>} entries, and boxed or
 * nullable values are represented through generated wrapper composites.</p>
 */
public final class SbeXmlGenerator {

    /**
     * Generates the XML schema text for the supplied root specification.
     */
    public String generate(final TypeSpec spec) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<sbe:messageSchema xmlns:sbe=\"http://fixprotocol.io/2016/sbe\" package=\"")
                .append(spec.javaType().getPackageName())
                .append("\" id=\"1\" version=\"0\" semanticVersion=\"0.1.0\" byteOrder=\"littleEndian\">\n");
        builder.append("    <types>\n");
        builder.append("        <composite name=\"messageHeader\">\n");
        builder.append("            <type name=\"blockLength\" primitiveType=\"uint16\"/>\n");
        builder.append("            <type name=\"templateId\" primitiveType=\"uint16\"/>\n");
        builder.append("            <type name=\"schemaId\" primitiveType=\"uint16\"/>\n");
        builder.append("            <type name=\"version\" primitiveType=\"uint16\"/>\n");
        builder.append("        </composite>\n");
        builder.append("        <composite name=\"varStringEncoding\">\n");
        builder.append("            <type name=\"length\" primitiveType=\"uint32\"/>\n");
        builder.append("            <type name=\"varData\" primitiveType=\"uint8\" length=\"0\" characterEncoding=\"UTF-8\"/>\n");
        builder.append("        </composite>\n");
        builder.append("        <composite name=\"varDataEncoding\">\n");
        builder.append("            <type name=\"length\" primitiveType=\"uint32\"/>\n");
        builder.append("            <type name=\"varData\" primitiveType=\"uint8\" length=\"0\"/>\n");
        builder.append("        </composite>\n");
        emitNullableWrappers(builder, spec, new HashSet<>(), new HashSet<>());
        emitNestedComposites(builder, spec, new HashSet<>());
        builder.append("    </types>\n");
        builder.append("    <message name=\"").append(spec.schemaName()).append("\" id=\"1\">\n");
        int id = 1;
        for (FieldSpec field : spec.fields()) {
            if (field.variableLength()) {
                continue;
            }
            builder.append("        <field name=\"")
                    .append(field.name())
                    .append("\" id=\"")
                    .append(id++)
                    .append("\" type=\"")
                    .append(xmlType(field))
                    .append("\"/>\n");
        }
        for (final FieldSpec field : spec.fields()) {
            if (!field.variableLength()) {
                continue;
            }
            builder.append("        <data name=\"")
                    .append(field.name())
                    .append("\" id=\"")
                    .append(id++)
                    .append("\" type=\"")
                    .append(field.kind() == FieldKind.STRING ? "varStringEncoding" : "varDataEncoding")
                    .append("\"/>\n");
        }
        builder.append("    </message>\n");
        builder.append("</sbe:messageSchema>\n");
        return builder.toString();
    }

    /**
     * Emits reusable wrapper composites for nullable values.
     */
    private void emitNullableWrappers(
            final StringBuilder builder,
            final TypeSpec spec,
            final Set<String> emittedWrappers,
            final Set<Class<?>> visitedTypes
    ) {
        if (!visitedTypes.add(spec.javaType())) {
            return;
        }
        for (final FieldSpec field : spec.fields()) {
            final String wrapperName = nullableWrapperName(field);
            if (wrapperName != null && emittedWrappers.add(wrapperName)) {
                builder.append("        <composite name=\"").append(wrapperName).append("\">\n");
                builder.append("            <type name=\"present\" primitiveType=\"uint8\"/>\n");
                if (field.kind() == FieldKind.NESTED_FIXED) {
                    builder.append("            <ref name=\"value\" type=\"").append(field.nestedType().schemaName()).append("\"/>\n");
                } else if (field.kind() == FieldKind.FIXED_STRING) {
                    builder.append("            <type name=\"length\" primitiveType=\"uint16\"/>\n");
                    builder.append("            <type name=\"value\" primitiveType=\"char\" length=\"")
                            .append(field.fixedLength())
                            .append("\" characterEncoding=\"ISO-8859-1\"/>\n");
                } else if (field.kind() == FieldKind.FIXED_BYTES) {
                    builder.append("            <type name=\"length\" primitiveType=\"uint16\"/>\n");
                    builder.append("            <type name=\"value\" primitiveType=\"uint8\" length=\"")
                            .append(field.fixedLength())
                            .append("\"/>\n");
                } else {
                    builder.append("            <type name=\"value\" primitiveType=\"").append(nullablePrimitiveType(field)).append("\"/>\n");
                }
                builder.append("        </composite>\n");
            }
            if (field.kind() == FieldKind.NESTED_FIXED && field.nestedType() != null) {
                emitNullableWrappers(builder, field.nestedType(), emittedWrappers, visitedTypes);
            }
        }
    }

    /**
     * Emits nested fixed-size composites used inline by the root message.
     */
    private void emitNestedComposites(final StringBuilder builder, final TypeSpec spec, final Set<Class<?>> visited) {
        for (FieldSpec field : spec.fields()) {
            if (field.kind() != FieldKind.NESTED_FIXED || field.nestedType() == null) {
                continue;
            }
            final TypeSpec nested = field.nestedType();
            if (!visited.add(nested.javaType())) {
                continue;
            }
            builder.append("        <composite name=\"").append(nested.schemaName()).append("\">\n");
            for (final FieldSpec nestedField : nested.fields()) {
                if (nestedField.variableLength()) {
                    continue;
                }
                final String primitiveType = primitiveXmlType(nestedField);
                if (primitiveType != null) {
                    builder.append("            <type name=\"")
                            .append(nestedField.name())
                            .append("\" primitiveType=\"")
                            .append(primitiveType)
                            .append("\"");
                    if (nestedField.kind() == FieldKind.FIXED_STRING || nestedField.kind() == FieldKind.FIXED_BYTES) {
                        builder.append(" length=\"").append(nestedField.fixedLength()).append("\"");
                        if (nestedField.kind() == FieldKind.FIXED_STRING) {
                            builder.append(" characterEncoding=\"ISO-8859-1\"");
                        }
                    }
                    builder.append("/>\n");
                } else {
                    builder.append("            <ref name=\"")
                            .append(nestedField.name())
                            .append("\" type=\"")
                            .append(xmlType(nestedField))
                            .append("\"/>\n");
                }
            }
            builder.append("        </composite>\n");
            emitNestedComposites(builder, nested, visited);
        }
    }

    /**
     * Resolves the effective XML type reference used by a message field.
     */
    private String xmlType(final FieldSpec field) {
        return switch (field.kind()) {
            case BOOLEAN -> "uint8";
            case CHAR -> "char";
            case BOXED_BOOLEAN, BOXED_CHAR, BOXED_PRIMITIVE, ENUM, FIXED_STRING, FIXED_BYTES, NESTED_FIXED ->
                    nullableWrapperName(field);
            default -> primitiveType(field.kind(), field.javaType());
        };
    }

    private String primitiveXmlType(final FieldSpec field) {
        return switch (field.kind()) {
            case PRIMITIVE, BOOLEAN, CHAR, FIXED_STRING, FIXED_BYTES -> primitiveType(field.kind(), field.javaType());
            default -> null;
        };
    }

    private String nullableWrapperName(final FieldSpec field) {
        return switch (field.kind()) {
            case BOXED_BOOLEAN -> "NullableBoolean";
            case BOXED_CHAR -> "NullableChar";
            case BOXED_PRIMITIVE ->
                    "Nullable" + capitalize(primitiveType(FieldKind.PRIMITIVE, unbox(field.javaType())));
            case ENUM -> "NullableEnumInt32";
            case FIXED_STRING -> "NullableFixedString" + field.fixedLength();
            case FIXED_BYTES -> "NullableFixedBytes" + field.fixedLength();
            case NESTED_FIXED -> "Nullable" + field.nestedType().schemaName();
            default -> null;
        };
    }

    private String nullablePrimitiveType(final FieldSpec field) {
        return switch (field.kind()) {
            case BOXED_BOOLEAN -> "uint8";
            case BOXED_CHAR -> "char";
            case BOXED_PRIMITIVE -> primitiveType(FieldKind.PRIMITIVE, unbox(field.javaType()));
            case ENUM -> "int32";
            default -> throw new IllegalArgumentException("No nullable primitive type for " + field.kind());
        };
    }

    private String primitiveType(final FieldKind kind, final Class<?> type) {
        if (kind == FieldKind.FIXED_STRING) {
            return "char";
        }
        if (kind == FieldKind.FIXED_BYTES) {
            return "uint8";
        }
        if (kind == FieldKind.BOXED_PRIMITIVE) {
            if (type == Byte.class) return "int8";
            if (type == Short.class) return "int16";
            if (type == Integer.class) return "int32";
            if (type == Long.class) return "int64";
            if (type == Float.class) return "float";
            if (type == Double.class) return "double";
        }
        if (type == byte.class) return "int8";
        if (type == short.class) return "int16";
        if (type == int.class) return "int32";
        if (type == long.class) return "int64";
        if (type == float.class) return "float";
        if (type == double.class) return "double";
        return "int32";
    }

    /**
     * Converts boxed numeric types to their primitive counterpart when a helper
     * method needs primitive sizing or naming rules.
     */
    private Class<?> unbox(final Class<?> boxed) {
        if (boxed == Byte.class) return byte.class;
        if (boxed == Short.class) return short.class;
        if (boxed == Integer.class) return int.class;
        if (boxed == Long.class) return long.class;
        if (boxed == Float.class) return float.class;
        if (boxed == Double.class) return double.class;
        throw new IllegalArgumentException("Unsupported boxed type " + boxed.getName());
    }

    private String capitalize(final String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}