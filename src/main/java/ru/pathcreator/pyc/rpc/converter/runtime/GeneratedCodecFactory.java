package ru.pathcreator.pyc.rpc.converter.runtime;

import java.util.Collection;

/**
 * Service-provider contract used by generated codec registries.
 */
public interface GeneratedCodecFactory {
    /**
     * Returns all codecs contributed by this factory.
     *
     * @return generated codec instances
     */
    Collection<GeneratedCodec<?>> codecs();
}