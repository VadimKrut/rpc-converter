package ru.pathcreator.pyc.rpc.converter.runtime;

/**
 * Runtime contract implemented by generated codecs.
 *
 * @param <T> Java DTO type handled by the codec
 */
public interface GeneratedCodec<T> {
    /**
     * Returns the Java DTO type served by this codec.
     *
     * @return DTO class
     */
    Class<T> javaType();

    /**
     * Returns the generation strategy used for this codec.
     *
     * @return codec kind
     */
    CodecKind kind();

    /**
     * Computes the payload size required to encode the supplied value.
     *
     * @param value DTO value to measure
     * @return encoded payload size in bytes
     */
    int measure(T value);

    /**
     * Encodes the supplied DTO into the destination buffer.
     *
     * @param value  DTO value to encode
     * @param buffer destination buffer
     * @param offset start offset in the destination buffer
     * @return number of bytes written
     */
    int encode(T value, byte[] buffer, int offset);

    /**
     * Decodes a DTO value from the supplied byte range.
     *
     * @param buffer source buffer
     * @param offset start offset in the source buffer
     * @param length encoded payload length
     * @return decoded DTO instance
     */
    T decode(byte[] buffer, int offset, int length);

    /**
     * Allocates a right-sized byte array and encodes the value into it.
     *
     * @param value DTO value to encode
     * @return freshly allocated payload bytes
     */
    default byte[] encodeToBytes(T value) {
        byte[] payload = new byte[measure(value)];
        encode(value, payload, 0);
        return payload;
    }
}