package net.imglib2.type.label;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static net.imglib2.type.label.LabelUtils.writeInt;

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

		final RandomAccess<LabelMultisetType> sourceAccess = source.randomAccess();

		final int numDim = source.numDimensions();
		final long[] sourceShape = new long[numDim];
		final long[] cellOffset = new long[numDim]; // not in units of cells
		final long[] totalOffset = new long[numDim]; // absolute, inside of cell

		for (int i = 0; i < numDim; i++) {
			cellOffset[i] = source.min(i);
			sourceShape[i] = source.max(i) + 1 - cellOffset[i]; // interval is inclusive
		}

		final ByteArrayOutputStream listEntryOffsets = new ByteArrayOutputStream();

		final LongMappedAccessData listEntryData = LongMappedAccessData.factory.createStorage(32);

		// list: create new list
		// list2: compare with other lists
		final LabelMultisetEntryList list = new LabelMultisetEntryList(listEntryData, 0);
		final LabelMultisetEntryList list2 = new LabelMultisetEntryList();

		int nextListOffset = 0;

		final TIntObjectHashMap<TIntArrayList> offsetsForHashes = new TIntObjectHashMap<>();

		final TLongArrayList argMax = new TLongArrayList();

		final LabelMultisetEntry singleZeroEntry = new LabelMultisetEntry(0, 1);

		final LabelMultisetEntry addRef = list.createRef();
		final LabelMultisetEntry entrySetRef = list.createRef();

		int nonEmptyListCount = 0;

		for (int d = 0; d < numDim; ) {

			list.createListAt(listEntryData, nextListOffset);

			System.arraycopy(cellOffset, 0, totalOffset, 0, numDim);

			// populate list with all entries from all types
			for (int g = 0; g < numDim; ) {

				final LabelMultisetType lmt = sourceAccess.setPositionAndGet(totalOffset);
				if ( lmt.getAccess().getListDataUsedSizeInBytes() == 0) {
					/* this cell is non-existent, skip to the next cell */
					break;
				}


				for (final LabelMultisetType.Entry<Label> labelEntry : lmt.entrySetWithRef(entrySetRef))
					list.add((LabelMultisetEntry)labelEntry, addRef);


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

			/* Empty lists are fine if the whole block is empty, since we delete it.
			 * But empty lists in a block that has other non-empty lists results in no label
			 * over the empty list areas. We could do this (actually, I think it's a good idea)
			 * but historically, empty/missing data was represented as ID `0`, or Label.BACKGROUND
			 *
			 *
			 * NOTE: I think it's better if this behavior is changed in the future to allow empty lists which
			 * map to Label.INVALID, so that Label.BACKGROUND isn't ambiguous whether intentional or missing. */
			if (list.isEmpty())
				list.add(singleZeroEntry);
			else nonEmptyListCount++;

			if (maxNumEntriesPerPixel > 0 && list.size() > maxNumEntriesPerPixel) {
				// change order of e2, e1 for decreasing sort by count
				list.sortByCount();
			// change order of e2, e1 for increasing sort by count
			list.sortByCount();
			if (maxNumEntriesPerPixel > 0 && list.size() > maxNumEntriesPerPixel)
				list.limitSize(maxNumEntriesPerPixel);
			final long max = list.get(list.size() - 1).getId();
			argMax.add(max);
			list.sortById();

			boolean makeNewList = true;
			final int hash = list.hashCode();

			TIntArrayList listOffsets = offsetsForHashes.get(hash);
			if (listOffsets == null) {
				listOffsets = new TIntArrayList();
				offsetsForHashes.put(hash, listOffsets);
			}
			for (int i = 0; i < listOffsets.size(); ++i) {
				final int offset = listOffsets.get(i);
				list2.referToDataAt(listEntryData, offset);
				if (list.equals(list2)) {
					makeNewList = false;
					writeInt(listEntryOffsets, offset, ByteOrder.BIG_ENDIAN);
					break;
				}
			}
			if (makeNewList) {
				writeInt(listEntryOffsets, nextListOffset, ByteOrder.BIG_ENDIAN);
				listOffsets.add(nextListOffset);
				nextListOffset += (int)list.getSizeInBytes();
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

		if (nonEmptyListCount == 0 )
			return new VolatileLabelMultisetArray(0, true, new long[]{Label.INVALID});

		final IntBuffer intBuffer = ByteBuffer.wrap(listEntryOffsets.toByteArray()).asIntBuffer();
		final int[] listOffsets = new int[intBuffer.limit()];
		intBuffer.rewind();
		intBuffer.get(listOffsets);
		return new VolatileLabelMultisetArray(listOffsets, listEntryData, nextListOffset, true, argMax.toArray());
	}

	public static int getSerializedVolatileLabelMultisetArraySize(final VolatileLabelMultisetArray array) {

		return VolatileLabelMultisetArray.getRequiredNumberOfBytes(0, array.getCurrentStorageArray(), (int) array.getListDataUsedSizeInBytes());
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

