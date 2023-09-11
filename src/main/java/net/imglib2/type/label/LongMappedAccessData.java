package net.imglib2.type.label;

/**
 * A {@link MappedAccessData} that stores {@link LongMappedAccess
 * LongMappedElements} in a {@code long[]} array.
 *
 * @author Tobias Pietzsch  &lt;tobias.pietzsch@gmail.com&gt;
 */
public class LongMappedAccessData implements MappedAccessData<LongMappedAccess> {

	/**
	 * The current data storage. This is changed when the array is
	 * {@link #resize(long) resized}.
	 */
	protected long[] data;

	private long size;

	public long[] getData() {

		return data;
	}

	@Override
	public LongMappedAccess createAccess() {

		return new LongMappedAccess(this, 0);
	}

	@Override
	public void updateAccess(final LongMappedAccess access, final long baseOffset) {

		access.setDataArray(this);
		access.setBaseOffset(baseOffset);
	}

	private long longSizeFromByteSize(final long byteSize) {

		return (byteSize + ByteUtils.LONG_SIZE - 1) / ByteUtils.LONG_SIZE;
	}

	/**
	 * Create a new array containing {@code numElements} elements of
	 * {@code bytesPerElement} bytes each.
	 */
	private LongMappedAccessData(final long size) {

		final long longSize = longSizeFromByteSize(size);
		if (longSize > Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"trying to create a " + getClass().getName() + " with more than " + ((long) ByteUtils.LONG_SIZE * Integer.MAX_VALUE) + " bytes.");

		this.size = size;
		this.data = new long[(int) longSize];
	}

	@Override
	public long size() {

		return size;
	}

	/**
	 * {@inheritDoc} The storage array is reallocated and the old contents
	 * copied over.
	 */
	@Override
	public void resize(final long size) {

		final long longSize = longSizeFromByteSize(size);
		if (longSize == longSizeFromByteSize(this.size))
			return;

		if (longSize > Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"trying to resize a " + getClass().getName() + " with more than " + ((long) ByteUtils.LONG_SIZE * Integer.MAX_VALUE) + " bytes.");

		final long[] datacopy = new long[(int) longSize];
		final int copyLength = Math.min(data.length, datacopy.length);
		System.arraycopy(data, 0, datacopy, 0, copyLength);
		this.data = datacopy;
		this.size = size;
	}

	/**
	 * A factory for {@link LongMappedAccessData}s.
	 */
	public static final MappedAccessData.Factory<LongMappedAccessData, LongMappedAccess> factory =
			new MappedAccessData.Factory<LongMappedAccessData, LongMappedAccess>() {

				@Override
				public LongMappedAccessData createStorage(final long size) {

					return new LongMappedAccessData(size);
				}

				@Override
				public LongMappedAccess createAccess() {

					return new LongMappedAccess(null, 0);
				}
			};
}
