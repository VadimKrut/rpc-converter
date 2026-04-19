package ru.pathcreator.pyc.rpc.converter.runtime;

/**
 * Little-endian byte-array helpers used by generated codecs.
 */
public final class BinaryIo {

    private BinaryIo() {
    }

    /**
     * Writes one byte to the destination buffer.
     */
    public static void putByte(final byte[] buffer, final int offset, final byte value) {
        buffer[offset] = value;
    }

    /**
     * Reads one byte from the source buffer.
     */
    public static byte getByte(final byte[] buffer, final int offset) {
        return buffer[offset];
    }

    /**
     * Writes a little-endian {@code short}.
     */
    public static void putShortLE(final byte[] buffer, final int offset, final short value) {
        buffer[offset] = (byte) value;
        buffer[offset + 1] = (byte) (value >>> 8);
    }

    /**
     * Reads a little-endian {@code short}.
     */
    public static short getShortLE(final byte[] buffer, final int offset) {
        return (short) ((buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
    }

    /**
     * Writes a little-endian {@code int}.
     */
    public static void putIntLE(final byte[] buffer, final int offset, final int value) {
        buffer[offset] = (byte) value;
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
    }

    /**
     * Reads a little-endian {@code int}.
     */
    public static int getIntLE(final byte[] buffer, final int offset) {
        return (buffer[offset] & 0xFF)
               | ((buffer[offset + 1] & 0xFF) << 8)
               | ((buffer[offset + 2] & 0xFF) << 16)
               | ((buffer[offset + 3] & 0xFF) << 24);
    }

    /**
     * Writes a little-endian {@code long}.
     */
    public static void putLongLE(final byte[] buffer, final int offset, final long value) {
        buffer[offset] = (byte) value;
        buffer[offset + 1] = (byte) (value >>> 8);
        buffer[offset + 2] = (byte) (value >>> 16);
        buffer[offset + 3] = (byte) (value >>> 24);
        buffer[offset + 4] = (byte) (value >>> 32);
        buffer[offset + 5] = (byte) (value >>> 40);
        buffer[offset + 6] = (byte) (value >>> 48);
        buffer[offset + 7] = (byte) (value >>> 56);
    }

    /**
     * Reads a little-endian {@code long}.
     */
    public static long getLongLE(final byte[] buffer, final int offset) {
        return ((long) buffer[offset] & 0xFF)
               | (((long) buffer[offset + 1] & 0xFF) << 8)
               | (((long) buffer[offset + 2] & 0xFF) << 16)
               | (((long) buffer[offset + 3] & 0xFF) << 24)
               | (((long) buffer[offset + 4] & 0xFF) << 32)
               | (((long) buffer[offset + 5] & 0xFF) << 40)
               | (((long) buffer[offset + 6] & 0xFF) << 48)
               | (((long) buffer[offset + 7] & 0xFF) << 56);
    }

    /**
     * Writes a little-endian {@code float}.
     */
    public static void putFloatLE(final byte[] buffer, final int offset, final float value) {
        putIntLE(buffer, offset, Float.floatToRawIntBits(value));
    }

    /**
     * Reads a little-endian {@code float}.
     */
    public static float getFloatLE(final byte[] buffer, final int offset) {
        return Float.intBitsToFloat(getIntLE(buffer, offset));
    }

    /**
     * Writes a little-endian {@code double}.
     */
    public static void putDoubleLE(final byte[] buffer, final int offset, final double value) {
        putLongLE(buffer, offset, Double.doubleToRawLongBits(value));
    }

    /**
     * Reads a little-endian {@code double}.
     */
    public static double getDoubleLE(final byte[] buffer, final int offset) {
        return Double.longBitsToDouble(getLongLE(buffer, offset));
    }

    /**
     * Copies the whole source byte array into the destination buffer.
     */
    public static void putBytes(final byte[] destination, final int offset, final byte[] source) {
        System.arraycopy(source, 0, destination, offset, source.length);
    }

    /**
     * Returns a copy of the requested byte range.
     */
    public static byte[] getBytes(final byte[] source, final int offset, final int length) {
        final byte[] copy = new byte[length];
        System.arraycopy(source, offset, copy, 0, length);
        return copy;
    }
}