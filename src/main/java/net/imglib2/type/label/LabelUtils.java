package net.imglib2.type.label;

import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
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

import static net.imglib2.type.label.ByteUtils.INT_SIZE;

public class LabelUtils {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	public static byte[] serializeLabelMultisetTypes(
			final Iterable<LabelMultisetType> lmts,
			final int numElements) {

		final LabelMultisetEntry entryReference = new LabelMultisetEntry(0, 1);

		final int entrySizeInBytes = entryReference.getSizeInBytes();

		final int argMaxSize = INT_SIZE; // in reality, the int value is always `0`, with size of 4 bytes
		final int offsetListSize = INT_SIZE * numElements;
		final int legacyListSize = INT_SIZE; //See NOTE below
		final int estimateEntryListSize = ((int)(1 + (5*Math.log(numElements))) * (legacyListSize + entrySizeInBytes));
		final int initialSize = argMaxSize + offsetListSize + estimateEntryListSize;
		final ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream(initialSize);
		/* No longer serialized out ArgMax; we do this by specifying the ArgMax size as 0;
		 * It's now calculated during deserializtaion instead.*/
		writeInt(dataBuffer, 0, ByteOrder.BIG_ENDIAN);

		final TIntIntHashMap listOffsets = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

		int nextListOffset = 0;

		int entriesInList = 0;
		final ByteArrayOutputStream entryList = new ByteArrayOutputStream(entrySizeInBytes);
		for (final LabelMultisetType lmt : lmts) {
			final int listHash = lmt.listHashCode();
			int offsetsForHash = listOffsets.get(listHash);
			if (offsetsForHash != listOffsets.getNoEntryValue()) {
				writeInt(dataBuffer, offsetsForHash, ByteOrder.BIG_ENDIAN);
			} else {
				for (final LabelMultisetEntry entry : lmt.entrySetWithRef(entryReference)) {
					/* NOTE: This is unnecessary data, but an artifact of prior serialization strategy.
					 * It used to be that the entry we iterator from here was added to a temporary LabelMultisetEntryList
					 * and then that list was serialized. However, the way this logic was written, the list would alawys
					 * only contain 1 element.
					 * We don't do this now, but unfortunately, this mean that we now how to serialize an unnecessary `1`
					 * integer to mock the size of the list that is expected during deserialization. */
					writeInt(entryList, 1, ByteOrder.nativeOrder()); //the old list was always only 1 element in size, stored as the first 4 bytes of a long
					for (int i = 0; i < entrySizeInBytes; i++) {
						entryList.write(entry.access.getByte(i));
					}
				}
				listOffsets.put(listHash, nextListOffset);
				writeInt(dataBuffer, nextListOffset, ByteOrder.BIG_ENDIAN);
				nextListOffset += 4 + entrySizeInBytes; //Another quirk to maintain size compatibility, see list NOTE above.
				entriesInList++;
			}
		}

		final byte[] entryListBytes = entryList.toByteArray();
		dataBuffer.write(entryListBytes, 0, entryListBytes.length);
		return dataBuffer.toByteArray();
	}

	private static void writeInt(ByteArrayOutputStream dataBuffer, int value, ByteOrder byteOrder) {
		dataBuffer.write(ByteBuffer.allocate(4).order(byteOrder).putInt(value).array(), 0, 4);
	}


	public static byte[] serializeLabelMultisetTypesOld(
			final Iterable<LabelMultisetType> lmts,
			final int numElements) {

		final int[] listEntryOffsets = new int[numElements];

		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(32);

		final LabelMultisetEntryList list = new LabelMultisetEntryList(listData, 0);
		final LabelMultisetEntryList list2 = new LabelMultisetEntryList();
		final TIntObjectHashMap<TIntArrayList> offsetsForHashes = new TIntObjectHashMap<>();
		final LabelMultisetEntry iteratorEntry = new LabelMultisetEntry(0, 1);
		final LabelMultisetEntry addEntry = new LabelMultisetEntry(0, 1);
		final LabelMultisetEntry tmpAddEntry = new LabelMultisetEntry(0, 1);

		int nextListOffset = 0;
		int o = 0;

		for (final LabelMultisetType lmt : lmts) {
			list.createListAt(listData, nextListOffset);

			for (final LabelMultisetEntry entry : lmt.entrySetWithRef(iteratorEntry)) {
				final long id = entry.getElement().id();
				addEntry.setId(id);
				addEntry.setCount(entry.getCount());
				list.add(addEntry, tmpAddEntry);
			}

			boolean makeNewList = true;
			final int hash = list.hashCode();
			TIntArrayList offsetsForHash = offsetsForHashes.get(hash);
			if (offsetsForHash != null) {
				for (final TIntIterator it = offsetsForHash.iterator(); it.hasNext(); ) {
					final int offset = it.next();
					list2.referToDataAt(listData, offset);
					if (list.equals(list2)) {
						makeNewList = false;
						listEntryOffsets[o++] = offset;
						break;
					}
				}
			}
			if (makeNewList) {
				final boolean insertNeeded = offsetsForHash == null;
				if (offsetsForHash == null)
					offsetsForHash = new TIntArrayList();

				offsetsForHash.add(nextListOffset);
				if (insertNeeded)
					offsetsForHashes.put(hash, offsetsForHash);

				listEntryOffsets[o++] = nextListOffset;
				nextListOffset += list.getSizeInBytes();
			}
		}

		final byte[] bytes = new byte[VolatileLabelMultisetArray.getRequiredNumberOfBytes(0, listEntryOffsets, nextListOffset)];

		final ByteBuffer bb = ByteBuffer.wrap(bytes);

		/* No longer serialized out ArgMax; we do this by specifying the ArgMax size as 0;
		 * It's now calculated during deserializtaion instead.*/
		bb.putInt(0);

		for (final int d : listEntryOffsets) {
			bb.putInt(d);
		}

		final byte[] test = new byte[nextListOffset];
		for (int i = 0; i < nextListOffset; ++i) {
			test[i] = ByteUtils.getByte(listData.data, i);
			bb.put(ByteUtils.getByte(listData.data, i));
		}

		return bytes;
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
		final int listDataSize = bytes.length - (AbstractLabelMultisetLoader.listOffsetsSizeInBytes(listEntryOffsets.length)
				+ AbstractLabelMultisetLoader.argMaxListSizeInBytes(argMax.length));
		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(listDataSize);

		for (int i = 0; i < listEntryOffsets.length; ++i) {
			listEntryOffsets[i] = bb.getInt();
		}

		for (int i = 0; i < listDataSize; ++i) {
			ByteUtils.putByte(bb.get(), listData.data, i);
		}

		if (argMaxSize == 0) {
			argMax = new long[listEntryOffsets.length];
			for (int i = 0; i < listEntryOffsets.length; i++) {
				final int listDataIdx = listEntryOffsets[i];
				final LabelMultisetEntryList lmel = new LabelMultisetEntryList();
				lmel.referToDataAt(listData, listDataIdx);
				argMax[i] = LabelUtils.getArgMax(lmel);
			}
		}

		return new VolatileLabelMultisetArray(listEntryOffsets, listData, listDataSize, true, argMax);
	}

	public static long getArgMax(final Collection<? extends Entry<Label>> labelMultisetEntries) {

		int maxCount = 0;
		long maxCountId = Label.INVALID;
		for (final Entry<Label> entry : labelMultisetEntries) {
			if (maxCount < entry.getCount()) {
				maxCount = entry.getCount();
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
