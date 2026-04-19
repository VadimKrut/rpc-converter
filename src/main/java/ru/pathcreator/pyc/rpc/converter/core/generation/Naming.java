package ru.pathcreator.pyc.rpc.converter.core.generation;

/**
 * Shared naming helpers for generated source code and resource identifiers.
 */
public final class Naming {

    private Naming() {
    }

    /**
     * Returns the simple generated codec class name for a DTO type.
     */
    public static String codecSimpleName(final Class<?> type) {
        return sanitize(type.getName()) + "GeneratedCodec";
    }

    /**
     * Rewrites type names into Java-identifier-safe fragments for generated
     * field and helper names.
     */
    public static String sanitize(final String value) {
        return value.replace('.', '_').replace('$', '_');
    }

    /**
     * Returns a source-friendly Java type name, including array syntax and
     * nested classes.
     */
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