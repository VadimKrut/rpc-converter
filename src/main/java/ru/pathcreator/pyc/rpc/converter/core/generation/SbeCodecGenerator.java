package ru.pathcreator.pyc.rpc.converter.core.generation;

import ru.pathcreator.pyc.rpc.converter.core.model.FieldKind;
import ru.pathcreator.pyc.rpc.converter.core.model.FieldSpec;
import ru.pathcreator.pyc.rpc.converter.core.model.InstantiationStyle;
import ru.pathcreator.pyc.rpc.converter.core.model.TypeSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SbeCodecGenerator {

    public String generate(final TypeSpec spec, final String packageName) {
        final String simpleName = Naming.codecSimpleName(spec.javaType());
        final StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.BinaryIo;\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.CodecKind;\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.GeneratedCodec;\n");
        source.append("import ru.pathcreator.pyc.rpc.converter.runtime.ReflectionSupport;\n");
        source.append("import java.lang.invoke.VarHandle;\n");
        source.append("import java.lang.reflect.Constructor;\n");
        source.append("import java.nio.charset.StandardCharsets;\n\n");
        source.append("public final class ").append(simpleName).append(" implements GeneratedCodec<").append(Naming.javaTypeName(spec.javaType())).append("> {\n");
        emitFieldHandles(source, spec);
        emitConstructors(source, spec, new HashSet<>());
        source.append("    @Override\n");
        source.append("    public Class<").append(Naming.javaTypeName(spec.javaType())).append("> javaType() {\n");
        source.append("        return ").append(Naming.javaTypeName(spec.javaType())).append(".class;\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public CodecKind kind() {\n");
        source.append("        return CodecKind.SBE;\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public int measure(").append(Naming.javaTypeName(spec.javaType())).append(" value) {\n");
        source.append("        if (value == null) throw new IllegalArgumentException(\"Message must not be null\");\n");
        source.append("        int size = ").append(fixedSize(spec)).append(";\n");
        for (FieldSpec field : spec.fields()) {
            if (field.kind() == FieldKind.STRING) {
                source.append("        size += measureString((String) ").append(handle(field)).append(".get(value));\n");
            } else if (field.kind() == FieldKind.BYTES) {
                source.append("        size += measureBytes((byte[]) ").append(handle(field)).append(".get(value));\n");
            }
        }
        source.append("        return size;\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public int encode(").append(Naming.javaTypeName(spec.javaType())).append(" value, byte[] buffer, int offset) {\n");
        source.append("        if (value == null) throw new IllegalArgumentException(\"Message must not be null\");\n");
        emitFixedEncode(source, spec, "value", "offset");
        source.append("        int variableCursor = offset + ").append(fixedSize(spec)).append(";\n");
        for (final FieldSpec field : spec.fields()) {
            if (field.kind() == FieldKind.STRING) {
                source.append("        variableCursor = writeString((String) ").append(handle(field)).append(".get(value), buffer, variableCursor);\n");
            } else if (field.kind() == FieldKind.BYTES) {
                source.append("        variableCursor = writeBytes((byte[]) ").append(handle(field)).append(".get(value), buffer, variableCursor);\n");
            }
        }
        source.append("        return variableCursor - offset;\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public ").append(Naming.javaTypeName(spec.javaType())).append(" decode(byte[] buffer, int offset, int length) {\n");
        emitDecode(source, spec, "buffer", "offset");
        source.append("    }\n\n");
        emitHelpers(source, spec);
        source.append("}\n");
        return source.toString();
    }

    private void emitFieldHandles(final StringBuilder source, final TypeSpec spec) {
        emitFieldHandlesRecursive(source, spec);
        source.append('\n');
    }

    private void emitFieldHandlesRecursive(final StringBuilder source, final TypeSpec spec) {
        for (final FieldSpec field : spec.fields()) {
            source.append("    private static final VarHandle ")
                    .append(handle(field))
                    .append(" = ReflectionSupport.varHandle(")
                    .append(Naming.javaTypeName(field.field().getDeclaringClass()))
                    .append(".class, \"")
                    .append(field.name())
                    .append("\", ")
                    .append(Naming.javaTypeName(field.javaType()))
                    .append(".class);\n");
            if (field.kind() == FieldKind.NESTED_FIXED) {
                emitFieldHandlesRecursive(source, field.nestedType());
            }
        }
    }

    private void emitConstructors(
            final StringBuilder source,
            final TypeSpec spec,
            final Set<Class<?>> visited
    ) {
        emitConstructorsRecursive(source, spec, visited);
        source.append('\n');
    }

    private void emitConstructorsRecursive(
            final StringBuilder source,
            final TypeSpec spec,
            final Set<Class<?>> visited
    ) {
        if (!visited.add(spec.javaType())) {
            return;
        }
        if (spec.instantiationStyle() == InstantiationStyle.NO_ARGS_CONSTRUCTOR) {
            source.append("    private static final Constructor<")
                    .append(Naming.javaTypeName(spec.javaType()))
                    .append("> ")
                    .append(constructorField(spec))
                    .append(" = ReflectionSupport.noArgsConstructor(")
                    .append(Naming.javaTypeName(spec.javaType()))
                    .append(".class);\n");
        }
        for (final FieldSpec field : spec.fields()) {
            if (field.kind() == FieldKind.NESTED_FIXED) {
                emitConstructorsRecursive(source, field.nestedType(), visited);
            }
        }
    }

    private void emitFixedEncode(
            final StringBuilder source,
            final TypeSpec spec,
            final String objectRef,
            final String baseOffsetRef
    ) {
        int cursor = 0;
        for (final FieldSpec field : spec.fields()) {
            if (field.variableLength()) {
                continue;
            }
            cursor = emitEncodeField(source, field, objectRef, baseOffsetRef, cursor);
        }
    }

    private int emitEncodeField(
            final StringBuilder source,
            final FieldSpec field,
            final String objectRef,
            final String baseOffsetRef,
            final int cursor
    ) {
        final String absolute = baseOffsetRef + " + " + cursor;
        return switch (field.kind()) {
            case PRIMITIVE -> emitPrimitiveEncode(source, field, objectRef, absolute, cursor);
            case BOXED_PRIMITIVE -> emitBoxedPrimitiveEncode(source, field, objectRef, baseOffsetRef, absolute, cursor);
            case BOOLEAN -> {
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (((boolean) ")
                        .append(handle(field)).append(".get(").append(objectRef).append(")) ? 1 : 0));\n");
                yield cursor + 1;
            }
            case BOXED_BOOLEAN -> {
                source.append("        Boolean ").append(local(field)).append(" = (Boolean) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        BinaryIo.putByte(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", (byte) (Boolean.TRUE.equals(").append(local(field)).append(") ? 1 : 0));\n");
                yield cursor + 2;
            }
            case CHAR -> {
                source.append("        char ").append(local(field)).append(" = (char) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        if (").append(local(field)).append(" > 255) throw new IllegalArgumentException(\"char field ").append(field.name()).append(" must fit in one byte for SBE\");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) ").append(local(field)).append(");\n");
                yield cursor + 1;
            }
            case BOXED_CHAR -> {
                source.append("        Character ").append(local(field)).append(" = (Character) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        if (").append(local(field)).append(" != null && ").append(local(field)).append(" > 255) throw new IllegalArgumentException(\"char field ").append(field.name()).append(" must fit in one byte for SBE\");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", (byte) (").append(local(field)).append(" == null ? 0 : ").append(local(field)).append("));\n");
                yield cursor + 2;
            }
            case ENUM -> {
                source.append("        Enum<?> ").append(local(field)).append(" = (Enum<?>) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        BinaryIo.putIntLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local(field)).append(" == null ? -1 : ").append(local(field)).append(".ordinal());\n");
                yield cursor + 5;
            }
            case FIXED_STRING -> {
                source.append("        String ").append(local(field)).append(" = (String) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        writeFixedString(").append(local(field)).append(", buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(field.fixedLength()).append(", \"").append(field.name()).append("\");\n");
                yield cursor + 3 + field.fixedLength();
            }
            case FIXED_BYTES -> {
                source.append("        byte[] ").append(local(field)).append(" = (byte[]) ").append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        writeFixedBytes(").append(local(field)).append(", buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(field.fixedLength()).append(", \"").append(field.name()).append("\");\n");
                yield cursor + 3 + field.fixedLength();
            }
            case NESTED_FIXED -> {
                source.append("        ").append(Naming.javaTypeName(field.javaType())).append(" ").append(local(field)).append(" = (").append(Naming.javaTypeName(field.javaType())).append(") ")
                        .append(handle(field)).append(".get(").append(objectRef).append(");\n");
                source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local(field)).append(" != null ? 1 : 0));\n");
                source.append("        if (").append(local(field)).append(" != null) {\n");
                emitFixedEncodeNested(source, field.nestedType(), local(field), baseOffsetRef + " + " + (cursor + 1));
                source.append("        }\n");
                yield cursor + 1 + fixedSize(field.nestedType());
            }
            default -> cursor;
        };
    }

    private void emitFixedEncodeNested(
            final StringBuilder source,
            final TypeSpec spec,
            final String objectRef,
            final String baseOffsetRef
    ) {
        int cursor = 0;
        for (final FieldSpec nested : spec.fields()) {
            cursor = emitEncodeField(source, nested, objectRef, baseOffsetRef, cursor);
        }
    }

    private int emitPrimitiveEncode(
            final StringBuilder source,
            final FieldSpec field,
            final String objectRef,
            final String absolute,
            final int cursor
    ) {
        final Class<?> type = field.javaType();
        if (type == byte.class) {
            source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 1;
        }
        if (type == short.class) {
            source.append("        BinaryIo.putShortLE(buffer, ").append(absolute).append(", (short) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 2;
        }
        if (type == int.class) {
            source.append("        BinaryIo.putIntLE(buffer, ").append(absolute).append(", (int) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 4;
        }
        if (type == long.class) {
            source.append("        BinaryIo.putLongLE(buffer, ").append(absolute).append(", (long) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 8;
        }
        if (type == float.class) {
            source.append("        BinaryIo.putFloatLE(buffer, ").append(absolute).append(", (float) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 4;
        }
        if (type == double.class) {
            source.append("        BinaryIo.putDoubleLE(buffer, ").append(absolute).append(", (double) ").append(handle(field)).append(".get(").append(objectRef).append("));\n");
            return cursor + 8;
        }
        return cursor;
    }

    private int emitBoxedPrimitiveEncode(
            final StringBuilder source,
            final FieldSpec field,
            final String objectRef,
            final String baseOffsetRef,
            final String absolute,
            final int cursor
    ) {
        final String local = local(field);
        source.append("        ").append(Naming.javaTypeName(field.javaType())).append(" ").append(local).append(" = (").append(Naming.javaTypeName(field.javaType())).append(") ")
                .append(handle(field)).append(".get(").append(objectRef).append(");\n");
        source.append("        BinaryIo.putByte(buffer, ").append(absolute).append(", (byte) (").append(local).append(" != null ? 1 : 0));\n");
        final Class<?> type = field.javaType();
        if (type == Byte.class) {
            source.append("        BinaryIo.putByte(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? (byte) 0 : ").append(local).append(");\n");
            return cursor + 2;
        }
        if (type == Short.class) {
            source.append("        BinaryIo.putShortLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? (short) 0 : ").append(local).append(");\n");
            return cursor + 3;
        }
        if (type == Integer.class) {
            source.append("        BinaryIo.putIntLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? 0 : ").append(local).append(");\n");
            return cursor + 5;
        }
        if (type == Long.class) {
            source.append("        BinaryIo.putLongLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? 0L : ").append(local).append(");\n");
            return cursor + 9;
        }
        if (type == Float.class) {
            source.append("        BinaryIo.putFloatLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? 0f : ").append(local).append(");\n");
            return cursor + 5;
        }
        if (type == Double.class) {
            source.append("        BinaryIo.putDoubleLE(buffer, ").append(baseOffsetRef).append(" + ").append(cursor + 1).append(", ").append(local).append(" == null ? 0d : ").append(local).append(");\n");
            return cursor + 9;
        }
        return cursor;
    }

    private void emitDecode(
            final StringBuilder source,
            final TypeSpec spec,
            final String bufferRef,
            final String offsetRef
    ) {
        final List<String> variableNames = new ArrayList<>();
        int cursor = 0;
        for (FieldSpec field : spec.fields()) {
            if (field.variableLength()) {
                continue;
            }
            String varName = "decoded_" + field.name();
            variableNames.add(varName);
            cursor = emitFixedDecode(source, field, bufferRef, offsetRef, cursor, varName);
        }
        source.append("        int variableCursor = ").append(offsetRef).append(" + ").append(fixedSize(spec)).append(";\n");
        for (final FieldSpec field : spec.fields()) {
            if (field.kind() == FieldKind.STRING) {
                String varName = "decoded_" + field.name();
                variableNames.add(varName);
                source.append("        String ").append(varName).append(" = readString(").append(bufferRef).append(", variableCursor);\n");
                source.append("        variableCursor += measureString(").append(varName).append(");\n");
            } else if (field.kind() == FieldKind.BYTES) {
                String varName = "decoded_" + field.name();
                variableNames.add(varName);
                source.append("        byte[] ").append(varName).append(" = readBytes(").append(bufferRef).append(", variableCursor);\n");
                source.append("        variableCursor += measureBytes(").append(varName).append(");\n");
            }
        }
        if (spec.instantiationStyle() == InstantiationStyle.RECORD) {
            source.append("        return new ").append(Naming.javaTypeName(spec.javaType())).append("(");
            for (int i = 0; i < spec.fields().size(); i++) {
                if (i > 0) {
                    source.append(", ");
                }
                source.append("decoded_").append(spec.fields().get(i).name());
            }
            source.append(");\n");
            return;
        }
        source.append("        ").append(Naming.javaTypeName(spec.javaType())).append(" value = ReflectionSupport.instantiate(").append(constructorField(spec)).append(");\n");
        for (FieldSpec field : spec.fields()) {
            source.append("        ").append(handle(field)).append(".set(value, ").append("decoded_").append(field.name()).append(");\n");
        }
        source.append("        return value;\n");
    }

    private int emitFixedDecode(
            final StringBuilder source,
            final FieldSpec field,
            final String bufferRef,
            final String offsetRef,
            final int cursor,
            final String variableName
    ) {
        final String absolute = offsetRef + " + " + cursor;
        return switch (field.kind()) {
            case PRIMITIVE -> emitPrimitiveDecode(source, field, bufferRef, absolute, cursor, variableName);
            case BOXED_PRIMITIVE -> emitBoxedPrimitiveDecode(source, field, bufferRef, offsetRef, cursor, variableName);
            case BOOLEAN -> {
                source.append("        boolean ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") != 0;\n");
                yield cursor + 1;
            }
            case BOXED_BOOLEAN -> {
                source.append("        Boolean ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") == 0 ? null : (BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(") != 0);\n");
                yield cursor + 2;
            }
            case CHAR -> {
                source.append("        char ").append(variableName).append(" = (char) (BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") & 0xFF);\n");
                yield cursor + 1;
            }
            case BOXED_CHAR -> {
                source.append("        Character ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") == 0 ? null : (char) (BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(") & 0xFF);\n");
                yield cursor + 2;
            }
            case ENUM -> {
                source.append("        ").append(Naming.javaTypeName(field.javaType())).append(" ").append(variableName).append(" = null;\n");
                source.append("        if (BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") != 0) {\n");
                source.append("            ").append(variableName).append(" = ").append(Naming.javaTypeName(field.javaType())).append(".values()[BinaryIo.getIntLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(")];\n");
                source.append("        }\n");
                yield cursor + 5;
            }
            case FIXED_STRING -> {
                source.append("        String ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") == 0 ? null : readFixedString(")
                        .append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(", ").append(field.fixedLength()).append(");\n");
                yield cursor + 3 + field.fixedLength();
            }
            case FIXED_BYTES -> {
                source.append("        byte[] ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") == 0 ? null : readFixedBytes(")
                        .append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(", ").append(field.fixedLength()).append(");\n");
                yield cursor + 3 + field.fixedLength();
            }
            case NESTED_FIXED -> {
                source.append("        ").append(Naming.javaTypeName(field.javaType())).append(" ").append(variableName).append(" = null;\n");
                source.append("        if (BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(") != 0) {\n");
                source.append("            ").append(variableName).append(" = decode").append(Naming.sanitize(field.javaType().getName())).append("(")
                        .append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
                source.append("        }\n");
                yield cursor + 1 + fixedSize(field.nestedType());
            }
            default -> cursor;
        };
    }

    private int emitPrimitiveDecode(
            final StringBuilder source,
            final FieldSpec field,
            final String bufferRef,
            final String absolute,
            final int cursor,
            final String variableName
    ) {
        final Class<?> type = field.javaType();
        if (type == byte.class) {
            source.append("        byte ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 1;
        }
        if (type == short.class) {
            source.append("        short ").append(variableName).append(" = BinaryIo.getShortLE(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 2;
        }
        if (type == int.class) {
            source.append("        int ").append(variableName).append(" = BinaryIo.getIntLE(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 4;
        }
        if (type == long.class) {
            source.append("        long ").append(variableName).append(" = BinaryIo.getLongLE(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 8;
        }
        if (type == float.class) {
            source.append("        float ").append(variableName).append(" = BinaryIo.getFloatLE(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 4;
        }
        if (type == double.class) {
            source.append("        double ").append(variableName).append(" = BinaryIo.getDoubleLE(").append(bufferRef).append(", ").append(absolute).append(");\n");
            return cursor + 8;
        }
        return cursor;
    }

    private int emitBoxedPrimitiveDecode(
            final StringBuilder source,
            final FieldSpec field,
            final String bufferRef,
            final String offsetRef,
            final int cursor,
            final String variableName
    ) {
        Class<?> type = field.javaType();
        if (type == Byte.class) {
            source.append("        Byte ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 2;
        }
        if (type == Short.class) {
            source.append("        Short ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getShortLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 3;
        }
        if (type == Integer.class) {
            source.append("        Integer ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getIntLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 5;
        }
        if (type == Long.class) {
            source.append("        Long ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getLongLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 9;
        }
        if (type == Float.class) {
            source.append("        Float ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getFloatLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 5;
        }
        if (type == Double.class) {
            source.append("        Double ").append(variableName).append(" = BinaryIo.getByte(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor).append(") == 0 ? null : BinaryIo.getDoubleLE(").append(bufferRef).append(", ").append(offsetRef).append(" + ").append(cursor + 1).append(");\n");
            return cursor + 9;
        }
        return cursor;
    }

    private void emitHelpers(final StringBuilder source, final TypeSpec spec) {
        source.append("    private static int measureString(String value) {\n");
        source.append("        return 4 + (value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length);\n");
        source.append("    }\n\n");
        source.append("    private static int measureBytes(byte[] value) {\n");
        source.append("        return 4 + (value == null ? 0 : value.length);\n");
        source.append("    }\n\n");
        source.append("    private static int writeString(String value, byte[] buffer, int offset) {\n");
        source.append("        if (value == null) {\n");
        source.append("            BinaryIo.putIntLE(buffer, offset, -1);\n");
        source.append("            return offset + 4;\n");
        source.append("        }\n");
        source.append("        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);\n");
        source.append("        BinaryIo.putIntLE(buffer, offset, bytes.length);\n");
        source.append("        BinaryIo.putBytes(buffer, offset + 4, bytes);\n");
        source.append("        return offset + 4 + bytes.length;\n");
        source.append("    }\n\n");
        source.append("    private static int writeBytes(byte[] value, byte[] buffer, int offset) {\n");
        source.append("        if (value == null) {\n");
        source.append("            BinaryIo.putIntLE(buffer, offset, -1);\n");
        source.append("            return offset + 4;\n");
        source.append("        }\n");
        source.append("        BinaryIo.putIntLE(buffer, offset, value.length);\n");
        source.append("        BinaryIo.putBytes(buffer, offset + 4, value);\n");
        source.append("        return offset + 4 + value.length;\n");
        source.append("    }\n\n");
        source.append("    private static void writeFixedString(String value, byte[] buffer, int offset, int fixedLength, String fieldName) {\n");
        source.append("        byte[] encoded = value == null ? null : encodeLatin1(value, fixedLength, fieldName);\n");
        source.append("        BinaryIo.putShortLE(buffer, offset, (short) (encoded == null ? 0 : encoded.length));\n");
        source.append("        int valueOffset = offset + 2;\n");
        source.append("        for (int i = 0; i < fixedLength; i++) {\n");
        source.append("            BinaryIo.putByte(buffer, valueOffset + i, (byte) 0);\n");
        source.append("        }\n");
        source.append("        if (encoded != null) {\n");
        source.append("            BinaryIo.putBytes(buffer, valueOffset, encoded);\n");
        source.append("        }\n");
        source.append("    }\n\n");
        source.append("    private static void writeFixedBytes(byte[] value, byte[] buffer, int offset, int fixedLength, String fieldName) {\n");
        source.append("        int length = value == null ? 0 : value.length;\n");
        source.append("        if (length > fixedLength) throw new IllegalArgumentException(\"byte[] field \" + fieldName + \" exceeds fixed SBE length \" + fixedLength);\n");
        source.append("        BinaryIo.putShortLE(buffer, offset, (short) length);\n");
        source.append("        int valueOffset = offset + 2;\n");
        source.append("        for (int i = 0; i < fixedLength; i++) {\n");
        source.append("            BinaryIo.putByte(buffer, valueOffset + i, (byte) 0);\n");
        source.append("        }\n");
        source.append("        if (value != null) {\n");
        source.append("            BinaryIo.putBytes(buffer, valueOffset, value);\n");
        source.append("        }\n");
        source.append("    }\n\n");
        source.append("    private static byte[] encodeLatin1(String value, int fixedLength, String fieldName) {\n");
        source.append("        byte[] encoded = value.getBytes(StandardCharsets.ISO_8859_1);\n");
        source.append("        if (!value.equals(new String(encoded, StandardCharsets.ISO_8859_1))) {\n");
        source.append("            throw new IllegalArgumentException(\"String field \" + fieldName + \" contains characters not representable in ISO-8859-1 fixed SBE encoding\");\n");
        source.append("        }\n");
        source.append("        if (encoded.length > fixedLength) throw new IllegalArgumentException(\"String field \" + fieldName + \" exceeds fixed SBE length \" + fixedLength);\n");
        source.append("        return encoded;\n");
        source.append("    }\n\n");
        source.append("    private static String readFixedString(byte[] buffer, int offset, int fixedLength) {\n");
        source.append("        int length = BinaryIo.getShortLE(buffer, offset) & 0xFFFF;\n");
        source.append("        return new String(buffer, offset + 2, Math.min(length, fixedLength), StandardCharsets.ISO_8859_1);\n");
        source.append("    }\n\n");
        source.append("    private static byte[] readFixedBytes(byte[] buffer, int offset, int fixedLength) {\n");
        source.append("        int length = BinaryIo.getShortLE(buffer, offset) & 0xFFFF;\n");
        source.append("        return BinaryIo.getBytes(buffer, offset + 2, Math.min(length, fixedLength));\n");
        source.append("    }\n\n");
        source.append("    private static String readString(byte[] buffer, int offset) {\n");
        source.append("        int length = BinaryIo.getIntLE(buffer, offset);\n");
        source.append("        if (length < 0) {\n");
        source.append("            return null;\n");
        source.append("        }\n");
        source.append("        return new String(buffer, offset + 4, length, StandardCharsets.UTF_8);\n");
        source.append("    }\n\n");
        source.append("    private static byte[] readBytes(byte[] buffer, int offset) {\n");
        source.append("        int length = BinaryIo.getIntLE(buffer, offset);\n");
        source.append("        if (length < 0) {\n");
        source.append("            return null;\n");
        source.append("        }\n");
        source.append("        return BinaryIo.getBytes(buffer, offset + 4, length);\n");
        source.append("    }\n\n");
        emitNestedDecodeHelpers(source, spec, new HashSet<>());
    }

    private void emitNestedDecodeHelpers(
            final StringBuilder source,
            final TypeSpec spec,
            final Set<Class<?>> emitted
    ) {
        for (final FieldSpec field : spec.fields()) {
            if (field.kind() != FieldKind.NESTED_FIXED) {
                continue;
            }
            final TypeSpec nested = field.nestedType();
            if (!emitted.add(nested.javaType())) {
                continue;
            }
            source.append("    private static ")
                    .append(Naming.javaTypeName(nested.javaType()))
                    .append(" decode")
                    .append(Naming.sanitize(nested.javaType().getName()))
                    .append("(byte[] buffer, int offset) {\n");
            emitDecode(source, nested, "buffer", "offset");
            source.append("    }\n\n");
            emitNestedDecodeHelpers(source, nested, emitted);
        }
    }

    private String handle(final FieldSpec field) {
        return "VH_" + Naming.sanitize(field.field().getDeclaringClass().getName()) + "_" + field.name();
    }

    private String constructorField(final TypeSpec spec) {
        return "CTOR_" + Naming.sanitize(spec.javaType().getName());
    }

    private String local(FieldSpec field) {
        return "value_" + field.name();
    }

    private int fixedSize(final TypeSpec spec) {
        int size = 0;
        for (final FieldSpec field : spec.fields()) {
            if (field.variableLength()) {
                continue;
            }
            size += switch (field.kind()) {
                case PRIMITIVE -> primitiveSize(field.javaType());
                case BOXED_PRIMITIVE -> 1 + primitiveSize(unbox(field.javaType()));
                case BOOLEAN -> 1;
                case BOXED_BOOLEAN -> 2;
                case CHAR -> 1;
                case BOXED_CHAR -> 2;
                case ENUM -> 5;
                case FIXED_STRING, FIXED_BYTES -> 3 + field.fixedLength();
                case NESTED_FIXED -> 1 + fixedSize(field.nestedType());
                default -> 0;
            };
        }
        return size;
    }

    private Class<?> unbox(final Class<?> boxed) {
        if (boxed == Byte.class) return byte.class;
        if (boxed == Short.class) return short.class;
        if (boxed == Integer.class) return int.class;
        if (boxed == Long.class) return long.class;
        if (boxed == Float.class) return float.class;
        if (boxed == Double.class) return double.class;
        throw new IllegalArgumentException("Unsupported boxed type " + boxed.getName());
    }

    private int primitiveSize(final Class<?> primitive) {
        if (primitive == byte.class) return 1;
        if (primitive == short.class) return 2;
        if (primitive == int.class || primitive == float.class) return 4;
        if (primitive == long.class || primitive == double.class) return 8;
        throw new IllegalArgumentException("Unsupported primitive type " + primitive.getName());
    }
}