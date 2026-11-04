package net.imglib2.type.label;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * Helper methods to encode and decode different data types
 * ({@code long, double} etc.) from bytes at an offset in a {@code long[]}
 * array.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Caleb Hulbert;
 */
public class ByteUtils {

	// @formatter:off
	/* some data is serialized little-endian (historic quirk of depending on native byte order) */
	private static final ValueLayout.OfByte   BYTE_LAYOUT         = ValueLayout.JAVA_BYTE;
	private static final ValueLayout.OfInt    INT_LE_UNALIGNED    = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private static final ValueLayout.OfLong   LONG_LE_UNALIGNED   = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private static final ValueLayout.OfFloat  FLOAT_LE_UNALIGNED  = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private static final ValueLayout.OfDouble DOUBLE_LE_UNALIGNED = ValueLayout.JAVA_DOUBLE_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	// @formatter:on

	public static void putByte(final byte value, final long[] array, final long offset) {
		MemorySegment.ofArray(array).set(BYTE_LAYOUT, offset, value);
	}

	public static byte getByte(final long[] array, final long offset) {
		return MemorySegment.ofArray(array).get(BYTE_LAYOUT, offset);
	}

	public static void putBoolean(final boolean value, final long[] array, final long offset) {
		putByte(value ? (byte) 1 : (byte) 0, array, offset);
	}

	public static boolean getBoolean(final long[] array, final long offset) {
		return getByte(array, offset) != (byte) 0;
	}

	public static void putInt(final int value, final long[] array, final long offset) {
		MemorySegment.ofArray(array).set(INT_LE_UNALIGNED, offset, value);
	}

	public static int getInt(final long[] array, final long offset) {
		return MemorySegment.ofArray(array).get(INT_LE_UNALIGNED, offset);
	}

	public static void putLong(final long value, final long[] array, final long offset) {
		MemorySegment.ofArray(array).set(LONG_LE_UNALIGNED, offset, value);
	}

	public static long getLong(final long[] array, final long offset) {
		return MemorySegment.ofArray(array).get(LONG_LE_UNALIGNED, offset);
	}

	public static void putFloat(final float value, final long[] array, final long offset) {
		MemorySegment.ofArray(array).set(FLOAT_LE_UNALIGNED, offset, value);
	}

	public static float getFloat(final long[] array, final long offset) {
		return MemorySegment.ofArray(array).get(FLOAT_LE_UNALIGNED, offset);
	}

	public static void putDouble(final double value, final long[] array, final long offset) {
		MemorySegment.ofArray(array).set(DOUBLE_LE_UNALIGNED, offset, value);
	}

	public static double getDouble(final long[] array, final long offset) {
		return MemorySegment.ofArray(array).get(DOUBLE_LE_UNALIGNED, offset);
	}

	// Note: offsets in bytes!
	public static void copyBytes(final long[] srcArray, final long srcOffset, final long[] dstArray, final long dstOffset, final int size) {
		MemorySegment.copy(MemorySegment.ofArray(srcArray), srcOffset, MemorySegment.ofArray(dstArray), dstOffset, size);
	}

	private static final VarHandle INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
	private static final VarHandle LONG_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

	public static void writeInt(final ByteArrayOutputStream dataBuffer, final int value, final ByteOrder byteOrder) {
		final byte[] bytes = new byte[Integer.BYTES];
		INT_LE.set(bytes, 0, byteOrder == ByteOrder.LITTLE_ENDIAN ? value : Integer.reverseBytes(value));
		dataBuffer.write(bytes, 0, bytes.length);
	}

	public static void writeLong(final ByteArrayOutputStream dataBuffer, final long value, final ByteOrder byteOrder) {
		final byte[] bytes = new byte[Long.BYTES];
		LONG_LE.set(bytes, 0, byteOrder == ByteOrder.LITTLE_ENDIAN ? value : Long.reverseBytes(value));
		dataBuffer.write(bytes, 0, bytes.length);
	}
}
