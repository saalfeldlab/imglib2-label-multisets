package net.imglib2.type.label;

import net.imglib2.img.NativeImg;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.AbstractNativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.label.RefList.RefIterator;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Fraction;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class LabelMultisetType extends AbstractNativeType<LabelMultisetType> implements IntegerType<LabelMultisetType> {

	public interface Entry<E> {

		E getElement();

		int getCount();
	}

	public static final LabelMultisetType type = new LabelMultisetType();

	private NativeImg<?, VolatileLabelMultisetArray> img;

	private VolatileLabelMultisetArray access;

	private final LabelMultisetEntryList entries;

	private final Set<Entry<Label>> entrySet;

	private LabelMultisetEntry reference = null;

	// this is the constructor if you want it to read from an array
	public LabelMultisetType(final NativeImg<?, VolatileLabelMultisetArray> img) {

		this(img, null);
	}

	// this is the constructor if you want to specify the dataAccess
	public LabelMultisetType(final VolatileLabelMultisetArray access) {

		this(null, access);
	}

	// this is the constructor if you want it to be a variable
	public LabelMultisetType() {

		this(null, new VolatileLabelMultisetArray(1, true, new long[]{Label.INVALID}));
	}

	// this is the constructor if you want it to be a variable
	public LabelMultisetType(final LabelMultisetEntry entry) {

		this();
		add(entry);
	}

	// this is the constructor if you want it to be a variable
	public LabelMultisetType(final LabelMultisetEntryList entries) {

		this();
		addAll(entries);
	}

	private LabelMultisetType(final NativeImg<?, VolatileLabelMultisetArray> img, final VolatileLabelMultisetArray access) {
		this(img, access, 0);
	}

	private LabelMultisetType(final NativeImg<?, VolatileLabelMultisetArray> img, final VolatileLabelMultisetArray access, final int idx) {

		this.entries = new LabelMultisetEntryList() {
			@Override
			public LabelMultisetEntry createRef() {
				if (reference == null) {
					return super.createRef();
				} else return reference;
			}

			@Override
			public void releaseRef(LabelMultisetEntry ref) {
				if (reference != ref) {
					super.releaseRef(ref);
				}
			}

			@Override
			public RefIterator<LabelMultisetEntry> iterator() {

				return new RefIterator<LabelMultisetEntry>() {

					private LabelMultisetEntry ref = reference != null ? reference : createRef();

					private int i = 0;

					@Override
					public boolean hasNext() {

						if (i < size())
							return true;
						else {
							release();
							return false;
						}
					}

					@Override
					public LabelMultisetEntry next() {

						return get(i++, ref);
					}

					@Override
					public void release() {

						if (reference == null && ref != null) {
							releaseRef(ref);
							ref = null;
						}
					}

					@Override
					public void reset() {

						if (reference == null && ref == null)
							ref = createRef();
						i = 0;
					}
				};
			}
		};

		this.img = img;
		this.access = access;
		this.i.set(idx);

		this.entrySet = new AbstractSet<Entry<Label>>() {

			private final RefIterator<Entry<Label>> iterator = new RefIterator<Entry<Label>>() {

				private final RefIterator<LabelMultisetEntry> it = entries.iterator();

				@Override
				public boolean hasNext() {

					return it.hasNext();
				}

				@Override
				public LabelMultisetEntry next() {

					return it.next();
				}


				@Override
				public void release() {
					if (reference == null) {
						it.release();
					}
				}

				@Override
				public void reset() {

					it.reset();
				}
			};

			@Override
			public RefIterator<Entry<Label>> iterator() {

				iterator.reset();
				return iterator;
			}

			@Override
			public int size() {

				return entries.size();
			}

			@Override
			public Stream<Entry<Label>> parallelStream() {

				throw new UnsupportedOperationException("Streams are not compatible with " + getClass().getName() + " because its iterator reuses the same reference.");
			}
		};
		if (this.access != null) {
			updateEntriesLocation();
		}
	}

	public void add(final long id, final int count) {

		add(new LabelMultisetEntry(id, count));
	}

	public void add(LabelMultisetEntry entry) {

		final Label label = entry.id;
		final LabelMultisetEntryList entryList = labelMultisetEntries();
		entryList.add(entry);

		final long argMax = argMax();
		final long id = label.id();
		if (id == argMax) return;
		if (entryList.size() == 1 || count(id) > count(argMax))
			updateArgMax(id);
	}

	public void addAll(Collection<? extends LabelMultisetEntry> entries) {

		labelMultisetEntries().addAll(entries);
		updateArgMax();
	}

	public void set(final long id, final int count) {

		labelMultisetEntries().clear();
		add(id, count);
	}

	public void set(Collection<? extends LabelMultisetEntry> entries) {

		labelMultisetEntries().clear();
		addAll(entries);
	}

	public void clear() {

		labelMultisetEntries().clear();
		updateArgMax();
	}

	@Override
	public Fraction getEntitiesPerPixel() {

		return new Fraction();
	}

	@Override
	public void updateContainer(final Object c) {

		access = img.update(c);
	}

	@Override
	public LabelMultisetType createVariable() {

		return new LabelMultisetType();
	}

	private LabelMultisetType(final LabelMultisetType source) {

		this(null, copySingleElement(source));
	}

	@Override
	public LabelMultisetType copy() {

		return new LabelMultisetType(this);
	}

	public int listHashCode() {
		return labelMultisetEntries().hashCode();
	}

	@Override
	public void set(final LabelMultisetType c) {

		if (c == this)
			return;

		final VolatileLabelMultisetArray singleElementCopy = copySingleElement(c);

		if (img != null) {
			addElementToImg(singleElementCopy);
		} else {
			i.set(0);
			access = singleElementCopy;
		}
		updateEntriesLocation();
	}

    /**
	 * at a high level, when setting a value on an img backed instance, we want to set the current element to point
	 * to a list equivalent to the type we are copying. To ensure we have the right size, we append the new list to
	 * the end, which may require growing the list. In that case,we also need to updat the `listDataUsedSizeInBytes`.
	 *
	 * TODO: this should be cleaner, more internal, and more systematic, but this is a first pass to address this.
	 *
	 * @param singleElementAccess
	 */
	private void addElementToImg(VolatileLabelMultisetArray singleElementAccess) {


		final LabelMultisetEntryList copiedEntries = new LabelMultisetEntryList();
		singleElementAccess.getValue(0, copiedEntries);

		/* handle the case when the used size in bytes is 0 or -1  */
		final long usedSize = access.getListDataUsedSizeInBytes();
		final LongMappedAccessData listData = access.getListData();
		final long offset = Math.max(usedSize < 0 ? listData.size() : usedSize, Integer.BYTES);
		final LabelMultisetEntryList appended = new LabelMultisetEntryList();

		/* the list grows `listData` itself, only when it runs out of room */
		appended.createListAt(listData, offset);
		appended.addAll(copiedEntries);
		access.getCurrentStorageArray()[i.get()] = Math.toIntExact(offset);
		access.setListDataUsedSizeInBytes(offset + appended.getSizeInBytes());
		access.setArgMax(i.get(), singleElementAccess.argMax(0));
	}

	@Override
	public NativeTypeFactory<LabelMultisetType, ?> getNativeTypeFactory() {

		throw new UnsupportedOperationException();
	}

	@Override
	public LabelMultisetType duplicateTypeOnSameNativeImg() {

		return new LabelMultisetType(img);
	}

	// ==== Multiset< SuperVoxel > =====

	public int size() {

		updateEntriesLocation();
		return entries.multisetSize();
	}

	public boolean isEmpty() {

		return labelMultisetEntries().isEmpty();
	}

	public boolean contains(final Label l, LabelMultisetEntry ref) {

		return contains(l.id(), ref);
	}

	public boolean contains(final Label l) {

		return contains(l.id());
	}

	public boolean contains(final long id, LabelMultisetEntry ref) {

		updateEntriesLocation();
		return entries.binarySearch(id, ref) >= 0;
	}

	public boolean contains(final long id) {

		updateEntriesLocation();
		return entries.binarySearch(id) >= 0;
	}

	public boolean containsAll(final long[] ids) {

		updateEntriesLocation();
		for (final long id : ids) {
			if (entries.binarySearch(id) < 0) {
				return false;
			}
		}
		return true;
	}

	public boolean containsAll(final long[] ids, LabelMultisetEntry ref) {

		updateEntriesLocation();
		for (final long id : ids) {
			if (entries.binarySearch(id, ref) < 0) {
				return false;
			}
		}
		return true;
	}

	public boolean containsAll(final Collection<? extends Label> c) {

		updateEntriesLocation();
		for (final Label l : c) {
			if (entries.binarySearch(l.id()) < 0) {
				return false;
			}
		}
		return true;
	}

	public boolean containsAll(final Collection<? extends Label> c, LabelMultisetEntry ref) {

		updateEntriesLocation();
		for (final Label l : c) {
			if (entries.binarySearch(l.id(), ref) < 0) {
				return false;
			}
		}
		return true;
	}

	public int count(final Label l) {

		return count(l.id());
	}

	public int count(final long id) {

		updateEntriesLocation();
		final int pos = entries.binarySearch(id);
		if (pos < 0) {
			return 0;
		}

		return entries.get(pos).getCount();
	}

	public int countWithRef(final long id, LabelMultisetEntry ref) {

		updateEntriesLocation();
		final int pos = entries.binarySearch(id, ref);
		if (pos < 0) {
			return 0;
		}

		return entries.get(pos).getCount();
	}

	public Set<Entry<Label>> entrySet() {

		updateEntriesLocation();
		return entrySet;
	}

	public Set<Entry<Label>> entrySetWithRef(LabelMultisetEntry ref) {
		reference = ref;
		return entrySet();
	}

	LabelMultisetEntryList labelMultisetEntries() {
		entrySet();
		return entries;
	}

	private void updateEntriesLocation() {
		if (access.getCurrentStorageArray().length == 0)
			return;
		access.getValue(i.get(), entries);
	}

	@Override
	public String toString() {

		updateEntriesLocation();
		return entries.toString();
	}

	// for volatile type
	boolean isValid() {

		return access.isValid();
	}

	@Override
	public boolean valueEquals(final LabelMultisetType other) {

		final LabelMultisetEntryList lmel = labelMultisetEntries();
		final LabelMultisetEntryList otherLmel = other.labelMultisetEntries();
		if (lmel.size() != otherLmel.size()) {
			return false;
		}

		final RefIterator<LabelMultisetEntry> ai = lmel.iterator();
		final RefIterator<LabelMultisetEntry> bi = otherLmel.iterator();

		while (ai.hasNext()) {
			final LabelMultisetEntry a = ai.next();
			final LabelMultisetEntry b = bi.next();
			if (!(a.getId() == b.getId() && a.getCount() == b.getCount())) {
				return false;
			}
		}
		return true;
	}

	@Override public boolean equals(Object obj) {

		if (obj instanceof LabelMultisetType) {
			return valueEquals((LabelMultisetType)obj);
		}
		return false;
	}

	public VolatileLabelMultisetArray getAccess() {

		return this.access;
	}

	@Override
	public void inc() {

		throw new UnsupportedOperationException();
	}

	@Override
	public void dec() {

		throw new UnsupportedOperationException();
	}

	@Override
	public double getMaxValue() {

		return argMax();
	}

	@Override
	public double getMinValue() {

		throw new UnsupportedOperationException();
	}

	@Override
	public double getMinIncrement() {

		throw new UnsupportedOperationException();
	}

	@Override
	public int getBitsPerPixel() {

		throw new UnsupportedOperationException();
	}

	@Override
	public double getRealDouble() {

		return argMax();
	}

	@Override
	public float getRealFloat() {

		return argMax();
	}

	@Override
	public double getImaginaryDouble() {

		return 0;
	}

	@Override
	public float getImaginaryFloat() {

		return 0;
	}

	@Override
	public void setReal(final float f) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setReal(final double f) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setImaginary(final float f) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setImaginary(final double f) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setComplexNumber(final float r, final float i) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setComplexNumber(final double r, final double i) {

		throw new UnsupportedOperationException();
	}

	@Override
	public float getPowerFloat() {

		return getRealFloat();
	}

	@Override
	public double getPowerDouble() {

		return getRealDouble();
	}

	@Override
	public float getPhaseFloat() {

		return 0;
	}

	@Override
	public double getPhaseDouble() {

		return 0;
	}

	@Override
	public void complexConjugate() {

		throw new UnsupportedOperationException();
	}

	@Override
	public void add(final LabelMultisetType c) {

		addAll(c.labelMultisetEntries());
	}

	@Override
	public void mul(final LabelMultisetType c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void sub(final LabelMultisetType c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void div(final LabelMultisetType c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void setOne() {

		set(1, 1);
	}

	@Override
	public void setZero() {

		set(0, 1);
	}

	@Override
	public void mul(final float c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void mul(final double c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(final LabelMultisetType arg0) {

		final long ours = argMax();
		final long theirs = arg0.argMax();

		final int argMaxCompare = Long.compare(ours, theirs);
		if (argMaxCompare != 0) return argMaxCompare;

		final int countCompare = Long.compare(count(ours), count(theirs));
		if (countCompare != 0) return countCompare;

		final ComparableLabelMultisetEntryList thisComparable = new ComparableLabelMultisetEntryList(labelMultisetEntries());
		final ComparableLabelMultisetEntryList otherComparable = new ComparableLabelMultisetEntryList(arg0.labelMultisetEntries());
		return thisComparable.compareTo(otherComparable);
	}

	@Override
	public int getInteger() {

		return (int) getIntegerLong();
	}

	@Override
	public long getIntegerLong() {

		return argMax();
	}

	@Override
	public BigInteger getBigInteger() {

		final BigInteger mask = new BigInteger("FFFFFFFFFFFFFFFF", 16);
		return BigInteger.valueOf(argMax()).and(mask);
	}

	@Override
	public void setInteger(final int f) {

		set(f, 1);
	}

	@Override
	public void setInteger(final long f) {

		set(f, 1);
	}

	@Override
	public void setBigInteger(final BigInteger b) {

		throw new UnsupportedOperationException();
	}

	public long argMax() {

		return this.access.argMax(i.get());
	}

	public void updateArgMax() {

		updateArgMax(LabelUtils.getArgMax(labelMultisetEntries()));
	}

	public void updateArgMax(long id) {

		this.access.setArgMax(i.get(), id);
	}

	public static LabelMultisetType singleEntryWithSingleOccurrence() {

		return singleEntryWithNumOccurrences(1);
	}

	public static LabelMultisetType singleEntryWithNumOccurrences(final int numOccurrences) {

		return new LabelMultisetType(new LabelMultisetEntry(Label.INVALID, numOccurrences));
	}

	@Override
	public void pow(final LabelMultisetType c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public void pow(final double d) {

		throw new UnsupportedOperationException();
	}

	/**
	 * Given a {@code source} LabelMultisetType, copy the single element that it currently refers to.
	 * <p>
	 * If {@code source.img} is not null, then only the specific element currently referenced is copied into the result.
	 * Otherwise, the {@code source} is a "variable" instance, whose access holds only that one element, and is copied as is.
	 *
	 * @param source the type to copy the current element of
	 * @return the single-element copy of the current element that {@code source} refers to
	 */
	private static VolatileLabelMultisetArray copySingleElement(final LabelMultisetType source) {

		if (source.img != null)
			return copySingleElementFromImg(source);
		else
			return copySingleElementFromVariable(source);
	}

	/**
	 * For a provided LabelMultisetType, copy the entire access (list data, indices, validity, and argMax), expecting the
	 * instance to be a "variable" instance, not an image view instance. That is, {@link #img} must be {@code null}.
	 *
	 * @param typeAsVariable the variable to copy from
	 * @return copy of the variable passed in
	 */
	private static VolatileLabelMultisetArray copySingleElementFromVariable(LabelMultisetType typeAsVariable) {

		/* copy the listData */
		final long byteSize = typeAsVariable.access.getListData().size();
		final int longSize = typeAsVariable.access.getListData().data.length;
		final LongMappedAccessData listDataCopy = LongMappedAccessData.factory.createStorage(byteSize);
		System.arraycopy(typeAsVariable.access.getListData().data, 0, listDataCopy.data, 0, longSize);

		/* copy the data */
		final int[] data = typeAsVariable.access.getCurrentStorageArray();
		final int[] dataCopy = new int[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);

		/* get a new access with all the copies */
		return new VolatileLabelMultisetArray(
				dataCopy,
				listDataCopy,
				typeAsVariable.access.getListDataUsedSizeInBytes(),
				typeAsVariable.access.isValid(),
				typeAsVariable.access.argMaxCopy());
	}

	/**
	 * For a provided LabelMultisetType that is backed by a non-null {@link #img}, return a new access
	 * that copies the entries and argMax for the current position in the
	 * {@link #img} that the provided {@code typeAsImgView} is backed by.
	 * <p>
	 * More explicitly, this will NOT copy the entire backing img, just the relevant data for the current position.
	 *
	 * @param typeAsImgView the image view to copy from
	 * @return the new access, equivalent to a "variable" instance of the provided image view instance
	 */
	private static VolatileLabelMultisetArray copySingleElementFromImg(LabelMultisetType typeAsImgView) {

		/* argmax max be different than the "calculated" argmax of the entries in cases where the entries
		* are empty. In those cases, the argmax is `INVALID` by default, but either from a custom loader,
		* or from the `fromBytes(long[], int, long)` fromBytes method, you can specify a non-default argmax
		* for empty elements. This is particularly common when it is preferable to have `BACKGROUND` as the
		* "missing element" value instead of `INVALID`*/
		long[] preexistingArgMax = {typeAsImgView.argMax()};
		final VolatileLabelMultisetArray access = new VolatileLabelMultisetArray(1, true, preexistingArgMax);
		final LabelMultisetEntryList entries = new LabelMultisetEntryList();

		access.getValue(0, entries);
		entries.addAll(typeAsImgView.labelMultisetEntries());
		return access;
	}

	public static class EmptyLabelMultisetTypeGenerator implements BiFunction<CellGrid, long[], byte[]> {

		private static int numElements(final int[] size) {

			int n = 1;
			for (final int s : size)
				n *= s;
			return n;
		}
		@Override
		public byte[] apply(final CellGrid cellGrid, final long[] cellPos) {

			final long[] cellMin = new long[cellPos.length];
			final int[] cellDims = new int[cellMin.length];
			Arrays.setAll(cellMin, d -> cellPos[d] * cellGrid.cellDimension(d));
			cellGrid.getCellDimensions(cellPos, cellMin, cellDims);
			final int numElements = numElements(cellDims);

			final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(0);
			final LabelMultisetEntryList list = new LabelMultisetEntryList(listData, 0);
			list.createListAt(listData, 0);
			final int listSize = (int)list.getSizeInBytes();

			final byte[] bytes = new byte[Integer.BYTES
					+ numElements * Long.BYTES // for argmaxes
					+ numElements * Integer.BYTES // for mappings
					+ listSize // for actual entries (one single entry)
					];

			final ByteBuffer bb = ByteBuffer.wrap(bytes);

			// argmax
			bb.putInt(numElements);
			for (int i = 0; i < listSize; ++i) {
				// ByteUtils.putByte( bb.get(), listData.data, i );
				bb.put(ByteUtils.getByte(listData.getData(), i));
			}
			return bytes;
		}
	}
}
