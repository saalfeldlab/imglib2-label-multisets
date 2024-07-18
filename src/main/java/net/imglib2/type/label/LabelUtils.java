package net.imglib2.type.label;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.label.LabelMultisetType.Entry;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

import static gnu.trove.impl.Constants.DEFAULT_CAPACITY;
import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;
import static net.imglib2.type.label.AbstractLabelMultisetLoader.argMaxListSizeInBytes;
import static net.imglib2.type.label.AbstractLabelMultisetLoader.listOffsetsSizeInBytes;
import static net.imglib2.type.label.ByteUtils.INT_SIZE;

public class LabelUtils {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static LabelMultisetType collapse(
			final Iterable<LabelMultisetType> lmts,
			final int numElements
	) {
		final LabelMultisetType result = new LabelMultisetType();
		collapse(lmts, numElements, result);
		return result;
	}

	public static void collapse(
			final Iterable<LabelMultisetType> lmts,
			final int numElements,
			final LabelMultisetType result
	) {
		result.entrySet().clear();
		final LabelMultisetEntry ref = new LabelMultisetEntry(0, 1);
		for (LabelMultisetType lmt : lmts) {
			result.entrySet().addAll(lmt.entrySetWithRef(ref));
		}
	}
	public static byte[] serializeLabelMultisetTypes(
			final Iterable<LabelMultisetType> lmts,
			final int numElements) {

		final LabelMultisetEntry entryReference = new LabelMultisetEntry(0, 1);

		final int argMaxSize = INT_SIZE; // in reality, the int value is always `0`, with size of 4 bytes
		final int offsetListSize = INT_SIZE * numElements;
		final ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
		/* No longer serialized out ArgMax; we do this by specifying the ArgMax size as 0;
		 * It's now calculated during deserializtaion instead.*/
		writeInt(dataBuffer, 0, ByteOrder.BIG_ENDIAN);

		final TIntIntHashMap listOffsets = new TIntIntHashMap(DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

		int nextListOffset = 0;

		int nonEmptyListCount = 0;

		final ByteArrayOutputStream entryList = new ByteArrayOutputStream();
		for (final LabelMultisetType lmt : lmts) {
			if (!(lmt.isEmpty() || (lmt.size() == 1 && lmt.argMax() == 0) ))
				nonEmptyListCount++;
			final int listHash = lmt.listHashCode();
			int listOffset = listOffsets.get(listHash);
			if (listOffset != listOffsets.getNoEntryValue()) {
				writeInt(dataBuffer, listOffset, ByteOrder.BIG_ENDIAN);
			} else {
				writeInt(entryList, lmt.entrySet().size(), ByteOrder.LITTLE_ENDIAN);
				for (final Entry<Label> entry : lmt.entrySetWithRef(entryReference)) {
					writeLong(entryList, entry.getElement().id(), ByteOrder.LITTLE_ENDIAN);
					writeInt(entryList, entry.getCount(), ByteOrder.LITTLE_ENDIAN);
				}
				listOffsets.put(listHash, nextListOffset);
				writeInt(dataBuffer, nextListOffset, ByteOrder.BIG_ENDIAN);
				nextListOffset = entryList.size(); //Another quirk to maintain size compatibility, see list NOTE above.
			}
		}

		if (nonEmptyListCount == 0)
			return null;

		final byte[] entryListBytes = entryList.toByteArray();
		dataBuffer.write(entryListBytes, 0, entryListBytes.length);
		return dataBuffer.toByteArray();
	}

	public static void writeInt(ByteArrayOutputStream dataBuffer, int value, ByteOrder byteOrder) {
		dataBuffer.write(ByteBuffer.allocate(4).order(byteOrder).putInt(value).array(), 0, 4);
	}

	public static void writeLong(ByteArrayOutputStream dataBuffer, long value, ByteOrder byteOrder) {
		dataBuffer.write(ByteBuffer.allocate(8).order(byteOrder).putLong(value).array(), 0, 8);
	}

	public static LabelMultisetType getOutOfBounds() {

		return getOutOfBounds(1);
	}

	public static LabelMultisetType getOutOfBounds(final int count) {

		return new LabelMultisetType(new LabelMultisetEntry(Label.OUTSIDE, count));
	}

	public static VolatileLabelMultisetArray fromBytes(final byte[] bytes, final int numElements) {

		final ByteBuffer bb = ByteBuffer.wrap(bytes);
		LOG.debug("Creating VolatileLabelMultisetArray from {} bytes for {} elements", bytes.length, numElements);

		final int argMaxSize = bb.getInt();
		LOG.debug("Data contains {} arg maxes", argMaxSize);
		long[] argMax = new long[argMaxSize];
		for (int i = 0; i < argMaxSize; ++i) {
			argMax[i] = bb.getLong();
		}

		final int[] listEntryOffsets = new int[numElements];
		final int listOffsetsSize = listOffsetsSizeInBytes(listEntryOffsets.length);
		final int argMaxListSize = argMaxListSizeInBytes(argMax.length);
		final int listDataSize = bytes.length - (listOffsetsSize + argMaxListSize);
		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(listDataSize);

		for (int i = 0; i < listEntryOffsets.length; ++i) {
			listEntryOffsets[i] = bb.getInt();
		}

		for (int i = 0; i < listDataSize; ++i) {
			ByteUtils.putByte(bb.get(), listData.data, i);
		}

		if (argMaxSize == 0) {
			argMax = new long[listEntryOffsets.length];
			final TIntLongHashMap entryOffsetToArgMax = new TIntLongHashMap(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, -1, -1);
			LabelMultisetEntryList lmel = null;
			for (int i = 0; i < listEntryOffsets.length; i++) {
				final int listDataIdx = listEntryOffsets[i];
				final long cachedArgMax = entryOffsetToArgMax.get(listDataIdx);
				if (cachedArgMax != entryOffsetToArgMax.getNoEntryValue()) {
					argMax[i] = cachedArgMax;
				} else {
					if (lmel == null) lmel = new LabelMultisetEntryList();
					lmel.referToDataAt(listData, listDataIdx);
					argMax[i] = LabelUtils.getArgMax(lmel);
					entryOffsetToArgMax.put(listDataIdx, argMax[i]);
				}
			}
		}

		return new VolatileLabelMultisetArray(listEntryOffsets, listData, listDataSize, true, argMax);
	}

	/**
	 * find the entry whose id has the highest count. If multiple entries has the same count, the
	 * lowest id is considered the argmax.
	 *
	 * @param labelMultisetEntries to search for the argmax in
	 * @return the id with the highest count
	 */
	public static long getArgMax(final Collection<? extends Entry<Label>> labelMultisetEntries) {

		int maxCount = 0;
		long maxCountId = Label.INVALID;
		for (final Entry<Label> entry : labelMultisetEntries) {
			final int count = entry.getCount();
			if (maxCount < count || maxCount == count && entry.getElement().id() < maxCountId) {
				maxCount = count;
				maxCountId = entry.getElement().id();
			}
		}
		return maxCountId;
	}

	public static RandomAccessibleInterval<UnsignedLongType> convertToUnsignedLong(
			final RandomAccessibleInterval<LabelMultisetType> labelMultisets) {

		return Converters.convert(
				labelMultisets,
				new LabelMultisetToUnsignedLongConverter(),
				new UnsignedLongType());
	}
}
