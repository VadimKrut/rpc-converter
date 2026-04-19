package ru.pathcreator.pyc.rpc.converter.core.generation;

public final class Naming {

    private Naming() {
    }

    public static String codecSimpleName(final Class<?> type) {
        return sanitize(type.getName()) + "GeneratedCodec";
    }

    public static String sanitize(final String value) {
        return value.replace('.', '_').replace('$', '_');
    }

    public static String javaTypeName(final Class<?> type) {
        if (type.isArray()) {
            return javaTypeName(type.getComponentType()) + "[]";
        }
        final String canonicalName = type.getCanonicalName();
        if (canonicalName != null) {
            return canonicalName;
        }
        return type.getName().replace('$', '.');
    }
}