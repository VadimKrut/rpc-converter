package ru.pathcreator.pyc.rpc.converter.runtime;

/**
 * Thrown when a generated codec registry cannot resolve a codec for a requested DTO type.
 */
public final class CodecNotFoundException extends RuntimeException {
    /**
     * Creates a new exception for the missing DTO type.
     *
     * @param type DTO class without a registered codec
     */
    public CodecNotFoundException(final Class<?> type) {
        super("No generated codec registered for " + type.getName());
    }
}