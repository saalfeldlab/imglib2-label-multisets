package net.imglib2.type.label;

/**
 * Maps into region of underlying memory area (a primitive array or similar). By
 * {@link MappedAccessData#updateAccess(MappedAccess, long)}, the base offset
 * of this {@link MappedAccess} in the memory area can be set. Values of
 * different types can be read or written at (byte) offsets relative to the
 * current base offset. For example {@code putLong( 42l, 2 )} would put write
 * the {@code long} value 42 into the bytes 2 ... 10 relative to the current
 * base offset.
 *
 * <p>
 * This is used to build imglib2-like proxy objects that map into primitive
 * arrays.
 *
 * <p>
 * Note: The method for updating the base offset
 * {@link MappedAccessData#updateAccess(MappedAccess, long)}} needs to be in
 * the {@link MappedAccessData}, not here. This is because data might be split
 * up into several {@link MappedAccessData MappedElementArrays}, in which case
 * the reference to the memory area must be updated in addition to the base
 * offset.
 *
 * @author Tobias Pietzsch &gt;tobias.pietzsch@gmail.com&lt;
 */
public interface MappedAccess<T extends MappedAccess<T>> {

  void putByte(final byte value, final int offset);

  byte getByte(final int offset);

  void putBoolean(final boolean value, final int offset);

  boolean getBoolean(final int offset);

  void putInt(final int value, final int offset);

  int getInt(final int offset);

  void putLong(final long value, final int offset);

  long getLong(final int offset);

  void putFloat(final float value, final int offset);

  float getFloat(final int offset);

  void putDouble(final double value, final int offset);

  double getDouble(final int offset);

  void copyFrom(final T fromAccess, final int numBytes);

  void swapWith(final T access, final int numBytes);
}
