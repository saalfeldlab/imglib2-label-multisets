package net.imglib2.type.label;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.label.LabelMultisetType.Entry;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Collection;

public class LabelUtils {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static byte[] serializeLabelMultisetTypes(
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

	for (int i = 0; i < nextListOffset; ++i) {
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

	final int[] data = new int[numElements];
	final int listDataSize = bytes.length - (AbstractLabelMultisetLoader.listOffsetsSizeInBytes(data.length)
			+ AbstractLabelMultisetLoader.argMaxListSizeInBytes(argMax.length));
	final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(listDataSize);

	for (int i = 0; i < data.length; ++i) {
	  data[i] = bb.getInt();
	}

	for (int i = 0; i < listDataSize; ++i) {
	  ByteUtils.putByte(bb.get(), listData.data, i);
	}

	if (argMaxSize == 0) {
	  argMax = new long[data.length];
	  for (int i = 0; i < data.length; i++) {
		final int listDataIdx = data[i];
		final LabelMultisetEntryList lmel = new LabelMultisetEntryList();
		lmel.referToDataAt(listData, listDataIdx);
		argMax[i] = LabelUtils.getArgMax(lmel);
	  }
	}

	return new VolatileLabelMultisetArray(data, listData, listDataSize, true, argMax);
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
