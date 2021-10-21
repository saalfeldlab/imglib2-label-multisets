package net.imglib2.type.label;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
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

	final int[] data = new int[numElements];
	final TLongArrayList argMax = new TLongArrayList();

	final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage(32);

	final LabelMultisetEntryList list = new LabelMultisetEntryList(listData, 0);
	final LabelMultisetEntryList list2 = new LabelMultisetEntryList();
	final TIntObjectHashMap<TIntArrayList> listHashesAndOffsets = new TIntObjectHashMap<>();
	final LabelMultisetEntry tentry = new LabelMultisetEntry(0, 1), ref = list.createRef();
	int nextListOffset = 0;
	int o = 0;
	for (final LabelMultisetType lmt : lmts) {
	  list.createListAt(listData, nextListOffset);

	  for (final Entry<Label> entry : lmt.entrySet()) {
		final long id = entry.getElement().id();
		tentry.setId(id);
		tentry.setCount(entry.getCount());
		list.add(tentry, ref);
	  }
	  argMax.add(lmt.argMax());

	  boolean makeNewList = true;
	  final int hash = list.hashCode();
	  TIntArrayList listOffsetsForHash = listHashesAndOffsets.get(hash);
	  if (listOffsetsForHash != null) {
		for (final TIntIterator it = listOffsetsForHash.iterator(); it.hasNext(); ) {
		  final int listOffset = it.next();
		  list2.referToDataAt(listData, listOffset);
		  if (list.equals(list2)) {
			makeNewList = false;
			data[o++] = listOffset;
			break;
		  }
		}
	  }
	  if (makeNewList) {
		final boolean insertNeeded = listOffsetsForHash == null;
		if (listOffsetsForHash == null)
		  listOffsetsForHash = new TIntArrayList();

		listOffsetsForHash.add(nextListOffset);
		if (insertNeeded)
		  listHashesAndOffsets.put(hash, listOffsetsForHash);

		data[o++] = nextListOffset;
		nextListOffset += list.getSizeInBytes();
	  }
	}
	list.releaseRef(ref);

	final byte[] bytes = new byte[VolatileLabelMultisetArray.getRequiredNumberOfBytes(argMax.size(), data, nextListOffset)];

	final ByteBuffer bb = ByteBuffer.wrap(bytes);

	bb.putInt(argMax.size());
	for (final TLongIterator it = argMax.iterator(); it.hasNext(); ) {
	  bb.putLong(it.next());
	}

	for (final int d : data) {
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
	final long[] argMax = new long[argMaxSize];
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
	return new VolatileLabelMultisetArray(data, listData, true, argMax);
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
