package ru.pathcreator.pyc.rpc.converter.runtime;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Loads generated codec factories through {@link ServiceLoader} and exposes typed codec lookup by DTO class.
 */
public final class GeneratedCodecRegistry {

    private final Map<Class<?>, GeneratedCodec<?>> codecs;

    private GeneratedCodecRegistry(final Map<Class<?>, GeneratedCodec<?>> codecs) {
        this.codecs = codecs;
    }

    /**
     * Loads all {@link GeneratedCodecFactory} providers visible to the current class loader.
     *
     * @return populated codec registry
     */
    public static GeneratedCodecRegistry load() {
        final ServiceLoader<GeneratedCodecFactory> loader = ServiceLoader.load(GeneratedCodecFactory.class);
        final Map<Class<?>, GeneratedCodec<?>> codecs = new LinkedHashMap<>();
        for (final GeneratedCodecFactory factory : loader) {
            final Collection<GeneratedCodec<?>> values = factory.codecs();
            for (final GeneratedCodec<?> codec : values) {
                codecs.put(codec.javaType(), codec);
            }
        }
        return new GeneratedCodecRegistry(codecs);
    }

    /**
     * Returns the generated codec for the supplied DTO type.
     *
     * @param type DTO class
     * @param <T>  DTO type
     * @return matching generated codec
     * @throws CodecNotFoundException if no codec is registered for the type
     */
    @SuppressWarnings("unchecked")
    public <T> GeneratedCodec<T> codecFor(final Class<T> type) {
        final GeneratedCodec<?> codec = codecs.get(type);
        if (codec == null) {
            throw new CodecNotFoundException(type);
        }
        return (GeneratedCodec<T>) codec;
    }
}