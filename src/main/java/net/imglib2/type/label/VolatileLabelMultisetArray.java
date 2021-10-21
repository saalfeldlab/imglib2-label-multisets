package net.imglib2.type.label;

import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;

public class VolatileLabelMultisetArray implements VolatileAccess, VolatileArrayDataAccess<VolatileLabelMultisetArray> {

  private boolean isValid = false;

  private final int[] data;

  private final long[] argMax;

  private final LongMappedAccessData listData;

  private final long listDataUsedSizeInBytes;

  public VolatileLabelMultisetArray(final int numEntities, final boolean isValid, final long[] argMax) {

	this.data = new int[numEntities];
	this.argMax = argMax;
	listData = LongMappedAccessData.factory.createStorage(16);
	listDataUsedSizeInBytes = 0;
	new MappedObjectArrayList<>(LabelMultisetEntry.type, listData, 0);
	this.isValid = isValid;
  }

  public VolatileLabelMultisetArray(
		  final int[] data,
		  final LongMappedAccessData listData,
		  final boolean isValid,
		  final long[] argMax) {

	this(data, listData, -1, isValid, argMax);
  }

  public VolatileLabelMultisetArray(
		  final int[] data,
		  final LongMappedAccessData listData,
		  final long listDataUsedSizeInBytes,
		  final boolean isValid,
		  final long[] argMax) {

	this.data = data;
	this.argMax = argMax;
	this.listData = listData;
	this.listDataUsedSizeInBytes = listDataUsedSizeInBytes;
	this.isValid = isValid;
  }

  public void getValue(final int index, final LabelMultisetEntryList ref) {

	ref.referToDataAt(listData, data[index]);
  }

  @Override
  public VolatileLabelMultisetArray createArray(final int numEntities) {

	return new VolatileLabelMultisetArray(numEntities, true, new long[]{Label.INVALID});
  }

  @Override
  public VolatileLabelMultisetArray createArray(final int numEntities, final boolean isValid) {

	return new VolatileLabelMultisetArray(numEntities, isValid, new long[]{Label.INVALID});
  }

  @Override
  public int[] getCurrentStorageArray() {

	return data;
  }

  public LongMappedAccessData getListData() {

	return listData;
  }

  public long getListDataUsedSizeInBytes() {

	return listDataUsedSizeInBytes;
  }

  @Override
  public boolean isValid() {

	return isValid;
  }

  public int getRequiredNumberOfBytes() {

	return getRequiredNumberOfBytes(this);
  }

  public static int getRequiredNumberOfBytes(final VolatileLabelMultisetArray array) {

	return getRequiredNumberOfBytes(array.argMax.length, array.data, (int)array.getListDataUsedSizeInBytes());
  }

  public static int getRequiredNumberOfBytes(final int numArgMax, final int[] listOffsets, final int listSizeInBytes) {

	return 0
			+ Integer.BYTES + Long.BYTES * numArgMax
			+ Integer.BYTES * listOffsets.length
			+ listSizeInBytes;
  }

  @Override
  public int getArrayLength() {

	return this.data.length;
  }

  public long argMax(final int offset) {

	return this.argMax[offset];
  }

  public void setArgMax(final int offset, final long val) {

	this.argMax[offset] = val;
  }

  public long[] argMaxCopy() {

	return this.argMax.clone();
  }

  private int toIndex(final int offset) {

	return data[offset];
  }

  // TODO do we need this? I do not think so but I am not 100% sure
  //	public static DefaultEmptyArrayCreator< VolatileLabelMultisetArray > emptyArrayCreator = new DefaultEmptyArrayCreator<>( new VolatileLabelMultisetArray( 1, false ) );
}
