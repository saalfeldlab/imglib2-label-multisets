package net.imglib2.type.label;

import gnu.trove.impl.Constants;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

import java.nio.ByteBuffer;

public class LabelMultisetTypeDownscaler {

  public static VolatileLabelMultisetArray createDownscaledCell(
		  final RandomAccessible<LabelMultisetType> source,
		  final Interval interval,
		  final int[] factor,
		  final int maxNumEntriesPerPixel) {

	return createDownscaledCell(Views.interval(source, interval), factor, maxNumEntriesPerPixel);
  }

  public static VolatileLabelMultisetArray createDownscaledCell(
		  final RandomAccessibleInterval<LabelMultisetType> source,
		  final int[] downscaleFactor,
		  final int maxNumEntriesPerPixel) {

	final RandomAccess<LabelMultisetType> randomAccess = source.randomAccess();

	final int numDim = source.numDimensions();
	final long[] sourceShape = new long[numDim];
	final long[] cellOffset = new long[numDim]; // not in units of cells
	final long[] totalOffset = new long[numDim]; // absolute, inside of cell

	for (int i = 0; i < numDim; i++) {
	  sourceShape[i] = source.max(i) + 1; // interval is inclusive
	}

	int numDownscaledLists = 1;
	for (int i = 0; i < numDim; i++) {
	  long cellDimLength = (long)Math.ceil((double)sourceShape[i] / downscaleFactor[i]);
	  numDownscaledLists *= cellDimLength;
	}

	final int[] listEntryOffsets = new int[numDownscaledLists];

	final LongMappedAccessData listEntryData = LongMappedAccessData.factory.createStorage(32);

	// list: create new list
	// list2: compare with other lists
	final LabelMultisetEntryList list = new LabelMultisetEntryList(listEntryData, 0);
	final LabelMultisetEntryList list2 = new LabelMultisetEntryList();

	final LabelMultisetEntry iteratorEntry = new LabelMultisetEntry(0, 1);

	final LabelMultisetEntry addEntry = new LabelMultisetEntry(0, 1);
	final LabelMultisetEntry tmpAddEntry = new LabelMultisetEntry(0, 1);

	int nextListOffset = 0;
	int o = 0;

	final TIntObjectHashMap<TIntArrayList> offsetsForHashes = new TIntObjectHashMap<>();

	final TLongArrayList argMax = new TLongArrayList();
	final TLongIntHashMap cellEntryMap = new TLongIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

	for (int d = 0; d < numDim; ) {

	  list.createListAt(listEntryData, nextListOffset);

	  System.arraycopy(cellOffset, 0, totalOffset, 0, numDim);

	  // populate list with all entries
	  for (int g = 0; g < numDim; ) {

		randomAccess.setPosition(totalOffset);

		for (LabelMultisetEntry sourceEntry : randomAccess.get().entrySetWithRef(iteratorEntry)) {
		  final long id = sourceEntry.getElement().id();
		  final int count = sourceEntry.getCount();
		  if (cellEntryMap.containsKey(id)) {
			cellEntryMap.put(id, cellEntryMap.get(id) + count);
		  } else {
			cellEntryMap.put(id, count);
		  }
		}

		/* This controls movement within the cell*/
		for (g = 0; g < numDim; g++) {
		  totalOffset[g] += 1;
		  if (totalOffset[g] < cellOffset[g] + downscaleFactor[g] && totalOffset[g] < sourceShape[g]) {
			break;
		  } else {
			totalOffset[g] = cellOffset[g];
		  }
		}
	  }

	  cellEntryMap.forEachEntry((id, count) -> {
		addEntry.setId(id);
		addEntry.setCount(count);
		list.add(addEntry, tmpAddEntry);
		return true;
	  });

	  cellEntryMap.clear();
	  list.sortById();

	  // sort by count and restrict to maxNumEntriesPerPixel (if
	  // applicable)
	  if (maxNumEntriesPerPixel > 0 && list.size() > maxNumEntriesPerPixel) {
		// change order of e2, e1 for decreasing sort by count
		list.sort((e1, e2) -> Long.compare(e2.getCount(), e1.getCount()));
		list.limitSize(maxNumEntriesPerPixel);
		list.sortById();
	  }
	  argMax.add(LabelUtils.getArgMax(list));

	  boolean makeNewList = true;
	  final int hash = list.hashCode();
	  TIntArrayList offsetsForHash = offsetsForHashes.get(hash);
	  if (offsetsForHash == null) {
		offsetsForHash = new TIntArrayList();
		offsetsForHashes.put(hash, offsetsForHash);
	  }
	  for (int i = 0; i < offsetsForHash.size(); ++i) {
		final int offset = offsetsForHash.get(i);
		list2.referToDataAt(listEntryData, offset);
		if (list.equals(list2)) {
		  makeNewList = false;
		  listEntryOffsets[o++] = offset;
		  break;
		}
	  }
	  if (makeNewList) {
		listEntryOffsets[o++] = nextListOffset;
		offsetsForHash.add(nextListOffset);
		nextListOffset += list.getSizeInBytes();

		// add entry with max count
	  }

	  /* this controls movement between cells */
	  for (d = 0; d < numDim; d++) {
		cellOffset[d] += downscaleFactor[d];
		if (cellOffset[d] < sourceShape[d]) {
		  break;
		} else {
		  cellOffset[d] = 0;
		}
	  }
	}
	return new VolatileLabelMultisetArray(listEntryOffsets, listEntryData, nextListOffset, true, argMax.toArray());
  }

  public static int getSerializedVolatileLabelMultisetArraySize(final VolatileLabelMultisetArray array) {

	return VolatileLabelMultisetArray.getRequiredNumberOfBytes(0, array.getCurrentStorageArray(), (int)array.getListDataUsedSizeInBytes());
  }

  public static void serializeVolatileLabelMultisetArray(final VolatileLabelMultisetArray array, final byte[] bytes) {

	final int[] curStorage = array.getCurrentStorageArray();
	final long[] data = array.getListData().data;

	final ByteBuffer bb = ByteBuffer.wrap(bytes);

	/* NOTE: We don't want to serialize the argMax, so indicate a `0` for it's size.
	 * 	This is necessary for backwards compatibility.
	 * 	The argMax is calulated during deserialization (check LabelUtils.fromBytes) */
	bb.putInt(0);

	for (final int d : curStorage) {
	  bb.putInt(d);
	}

	for (long i = 0; i < array.getListDataUsedSizeInBytes(); i++) {
	  bb.put(ByteUtils.getByte(data, i));
	}

  }

}

