package net.imglib2.type.label;

import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;

public class VolatileLabelMultisetArray implements VolatileAccess, VolatileArrayDataAccess< VolatileLabelMultisetArray >
{
	private boolean isValid = false;

	private final int[] data;

	private final MappedAccessData< LongMappedAccess > listData;

	private final long listDataUsedSizeInBytes;

	private final TLongHashSet containedLabels;

	public VolatileLabelMultisetArray( final int numEntities, final boolean isValid )
	{
		this.data = new int[ numEntities ];
		listData = LongMappedAccessData.factory.createStorage( 16 );
		listDataUsedSizeInBytes = 0;
		new MappedObjectArrayList<>( LabelMultisetEntry.type, listData, 0 ).add( new LabelMultisetEntry() );
		this.isValid = isValid;
		this.containedLabels = new TLongHashSet();
	}

	public VolatileLabelMultisetArray(
			final int[] data,
			final MappedAccessData< LongMappedAccess > listData,
			final boolean isValid,
			final TLongHashSet containedLabels )
	{
		this( data, listData, -1, isValid, containedLabels );
	}

	public VolatileLabelMultisetArray(
			final int[] data,
			final MappedAccessData< LongMappedAccess > listData,
			final long listDataUsedSizeInBytes,
			final boolean isValid,
			final TLongHashSet containedLabels )
	{
		this.data = data;
		this.listData = listData;
		this.listDataUsedSizeInBytes = listDataUsedSizeInBytes;
		this.isValid = isValid;
		this.containedLabels = containedLabels;
	}

	public void getValue( final int index, final LabelMultisetEntryList ref )
	{
		ref.referToDataAt( listData, data[ index ] );
	}

	@Override
	public VolatileLabelMultisetArray createArray( final int numEntities )
	{
		return new VolatileLabelMultisetArray( numEntities, true );
	}

	@Override
	public VolatileLabelMultisetArray createArray( final int numEntities, final boolean isValid )
	{
		return new VolatileLabelMultisetArray( numEntities, isValid );
	}

	@Override
	public int[] getCurrentStorageArray()
	{
		return data;
	}

	public MappedAccessData< LongMappedAccess > getListData()
	{
		return listData;
	}

	public long getListDataUsedSizeInBytes()
	{
		return listDataUsedSizeInBytes;
	}

	@Override
	public boolean isValid()
	{
		return isValid;
	}

	public boolean containsLabel( final long label )
	{
		return this.containedLabels.contains( label );
	}

	public int numContainedLabels()
	{
		return this.containedLabels.size();
	}

	public long[] containedLabels()
	{
		return this.containedLabels.toArray();
	}

	public int getRequiredNumberOfBytes()
	{
		return getRequiredNumberOfBytes( this );
	}

	public static int getRequiredNumberOfBytes( final VolatileLabelMultisetArray array )
	{
		return getRequiredNumberOfBytes( array.containedLabels.size(), array.data, ( int ) array.getListDataUsedSizeInBytes() );
	}

	public static int getRequiredNumberOfBytes( final int numberOfContainedLabels, final int[] listOffsets, final int listSizeInBytes )
	{
		return Integer.BYTES + Long.BYTES * numberOfContainedLabels + Integer.BYTES * listOffsets.length + listSizeInBytes;
	}

	@Override
	public int getArrayLength()
	{
		return this.data.length;
	}

	// TODO do we need this? I do not think so but I am not 100% sure
//	public static DefaultEmptyArrayCreator< VolatileLabelMultisetArray > emptyArrayCreator = new DefaultEmptyArrayCreator<>( new VolatileLabelMultisetArray( 1, false ) );
}
