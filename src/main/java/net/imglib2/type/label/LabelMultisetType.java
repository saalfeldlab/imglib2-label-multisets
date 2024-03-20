package net.imglib2.type.label;

import net.imglib2.Interval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.AbstractNativeType;
import net.imglib2.type.Index;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.label.RefList.RefIterator;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
			updateArgMax();
		}
	}

	public void add(final long id, final int count) {

		add(new LabelMultisetEntry(id, count));
	}

	public void add(LabelMultisetEntry entry) {

		labelMultisetEntries().add(entry);
		updateArgMax();
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
		if (index().get() >= 0) {
			updateArgMax();
		}
	}

	@Override
	public LabelMultisetType createVariable() {

		return new LabelMultisetType();
	}

	@Override
	public LabelMultisetType copy() {


		if (img != null) {
			/* If backed by an image, copy the entries only, not the entire backing data. */
			final LabelMultisetType labelMultisetType = new LabelMultisetType();
			labelMultisetType.labelMultisetEntries().addAll(labelMultisetEntries());
			return labelMultisetType;
		} else {
			/* copy the listData */
			final long byteSize = access.getListData().size();
			final int longSize = access.getListData().data.length;
			final LongMappedAccessData listDataCopy = LongMappedAccessData.factory.createStorage(byteSize);
			System.arraycopy(access.getListData().data, 0, listDataCopy.data, 0, longSize);

			/* copy the data */
			final int[] data = access.getCurrentStorageArray();
			final int[] dataCopy = new int[data.length];
			System.arraycopy(data, 0, dataCopy, 0, data.length);

			/* get a new access with all the copies */
			final VolatileLabelMultisetArray accessCopy = new VolatileLabelMultisetArray(
					dataCopy,
					listDataCopy,
					access.getListDataUsedSizeInBytes(),
					access.isValid(),
					access.argMaxCopy());
			/* get a new type instance */
			final LabelMultisetType that = new LabelMultisetType(null, accessCopy);
			return that;
		}
	}

	public int listHashCode() {
		return labelMultisetEntries().hashCode();
	}

	@Override
	public void set(final LabelMultisetType c) {

		if (c.img != null) {
			/* If backed by an image, copy the entries only, not the entire backing data. */

			img = null;
			i.set(0);
			clear();

			addAll(c.labelMultisetEntries());
		} else {
			/* copy the listData */
			final long byteSize = c.access.getListData().size();
			final int longSize = c.access.getListData().data.length;
			final LongMappedAccessData listDataCopy = LongMappedAccessData.factory.createStorage(byteSize);
			System.arraycopy(c.access.getListData().data, 0, listDataCopy.data, 0, longSize);

			/* copy the data */
			final int[] data = c.access.getCurrentStorageArray();
			final int[] dataCopy = new int[data.length];
			System.arraycopy(data, 0, dataCopy, 0, data.length);

			/* get a new access with all the copies */
			final VolatileLabelMultisetArray accessCopy = new VolatileLabelMultisetArray(
					dataCopy,
					listDataCopy,
					c.access.getListDataUsedSizeInBytes(),
					c.access.isValid(),
					c.access.argMaxCopy());
			/* get a new type instance */

			img = null;
			i.set(0);
			access = accessCopy;
			updateEntriesLocation();
		}
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
		final int initialComparison = Long.compare(ours, theirs);

		if (initialComparison != 0) return initialComparison;
		else return Long.compare(count(ours), count(theirs));
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

		this.access.setArgMax(i.get(), LabelUtils.getArgMax(labelMultisetEntries()));
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
