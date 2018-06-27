package net.imglib2.type.label;

import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;

public class VolatileLabelMultisetArray implements VolatileAccess, VolatileArrayDataAccess< VolatileLabelMultisetArray >
{
	private boolean isValid = false;

	private final int[] data;

	private final long[] argMax;

	private final MappedAccessData< LongMappedAccess > listData;

	private final long listDataUsedSizeInBytes;

	private final TLongHashSet containedLabels;

	public VolatileLabelMultisetArray( final int numEntities, final boolean isValid, final long[] argMax )
	{
		this.data = new int[ numEntities ];
		this.argMax = argMax;
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
			final TLongHashSet containedLabels,
			final long[] argMax )
	{
		this( data, listData, -1, isValid, containedLabels, argMax );
	}

	public VolatileLabelMultisetArray(
			final int[] data,
			final MappedAccessData< LongMappedAccess > listData,
			final long listDataUsedSizeInBytes,
			final boolean isValid,
			final TLongHashSet containedLabels,
			final long[] argMax )
	{
		this.data = data;
		this.argMax = argMax;
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
		return new VolatileLabelMultisetArray( numEntities, true, new long[] { Label.INVALID } );
	}

	@Override
	public VolatileLabelMultisetArray createArray( final int numEntities, final boolean isValid )
	{
		return new VolatileLabelMultisetArray( numEntities, isValid, new long[] { Label.INVALID } );
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
		return getRequiredNumberOfBytes( array.containedLabels.size(), array.argMax.length, array.data, ( int ) array.getListDataUsedSizeInBytes() );
	}

	public static int getRequiredNumberOfBytes( final int numberOfContainedLabels, final int numArgMax, final int[] listOffsets, final int listSizeInBytes )
	{
		return 0
				+ Integer.BYTES + Long.BYTES * numberOfContainedLabels
				+ Integer.BYTES + Long.BYTES * numArgMax
				+ Integer.BYTES * listOffsets.length
				+ listSizeInBytes;
	}

	@Override
	public int getArrayLength()
	{
		return this.data.length;
	}

	public long argMax( final int offset )
	{
		return this.argMax[ toIndex( offset ) ];
	}

	public void setArgMax( final int offset, final long val )
	{
		this.argMax[ toIndex( offset ) ] = val;
	}

	public long[] argMaxCopy()
	{
		return this.argMax.clone();
	}

	private int toIndex( final int offset )
	{
		return data[ offset ];
	}

	// TODO do we need this? I do not think so but I am not 100% sure
//	public static DefaultEmptyArrayCreator< VolatileLabelMultisetArray > emptyArrayCreator = new DefaultEmptyArrayCreator<>( new VolatileLabelMultisetArray( 1, false ) );
}
