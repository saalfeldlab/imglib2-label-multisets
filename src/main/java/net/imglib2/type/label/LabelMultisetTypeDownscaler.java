package net.imglib2.type.label;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.label.Multiset.Entry;
import net.imglib2.view.Views;

public class LabelMultisetTypeDownscaler
{

	public static VolatileLabelMultisetArray createDownscaledCell(
			final RandomAccessible< LabelMultisetType > source,
			final Interval interval,
			final int[] factor,
			final int maxNumEntriesPerPixel )
	{
		return createDownscaledCell( Views.interval( source, interval ), factor, maxNumEntriesPerPixel );
	}

	public static VolatileLabelMultisetArray createDownscaledCell(
			final RandomAccessible< LabelMultisetType > source,
			final Interval interval,
			final int[] factor,
			final TLongHashSet precomputedContainedLabels,
			final int maxNumEntriesPerPixel )
	{
		return createDownscaledCell( Views.interval( source, interval ), factor, precomputedContainedLabels, maxNumEntriesPerPixel );
	}

	public static VolatileLabelMultisetArray createDownscaledCell(
			final RandomAccessibleInterval< LabelMultisetType > source,
			final int[] factor,
			final int maxNumEntriesPerPixel )
	{
		final TLongHashSet set = new TLongHashSet();
		return createDownscaledCell( source, factor, set, set::add, maxNumEntriesPerPixel );
	}

	public static VolatileLabelMultisetArray createDownscaledCell(
			final RandomAccessibleInterval< LabelMultisetType > source,
			final int[] factor,
			final TLongHashSet precomputedContainedLabels,
			final int maxNumEntriesPerPixel )
	{
		return createDownscaledCell( source, factor, precomputedContainedLabels, id -> {}, maxNumEntriesPerPixel );
	}

	private static VolatileLabelMultisetArray createDownscaledCell(
			final RandomAccessibleInterval< LabelMultisetType > source,
			final int[] factor,
			final TLongHashSet labelsInBlock,
			final LongConsumer addToList,
			final int maxNumEntriesPerPixel )
	{

		final RandomAccess< LabelMultisetType > randomAccess = source.randomAccess();

		final int numDim = source.numDimensions();
		final long[] maxOffset = new long[ numDim ];
		final long[] cellOffset = new long[ numDim ]; // not in units of cells
		final long[] totalOffset = new long[ numDim ]; // absolute, inside of
														// cell

		for ( int i = 0; i < numDim; i++ )
			maxOffset[ i ] = source.max( i ) + 1; // interval is inclusive

		int numDownscaledLists = 1;
		for ( int i = 0; i < numDim; i++ )
			numDownscaledLists *= ( long ) Math.ceil( ( double ) maxOffset[ i ] / factor[ i ] );
		final int[] data = new int[ numDownscaledLists ];

		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage( 32 );

		// list: create new list
		// list2: compare with other lists
		final LabelMultisetEntryList list = new LabelMultisetEntryList( listData, 0 );
		final LabelMultisetEntryList list2 = new LabelMultisetEntryList();
		final LabelMultisetEntry entry = new LabelMultisetEntry( 0, 1 );
		int nextListOffset = 0;
		int o = 0;

		final TIntObjectHashMap< TIntArrayList > offsetsForHashes = new TIntObjectHashMap<>();

		for ( int d = 0; d < numDim; )
		{

			list.createListAt( listData, nextListOffset );

			for ( int i = 0; i < numDim; i++ )
				totalOffset[ i ] = cellOffset[ i ];

			// populate list with all entries
			for ( int g = 0; g < numDim; )
			{

				randomAccess.setPosition( totalOffset );
				for ( final Entry< Label > sourceEntry : randomAccess.get().entrySet() )
				{
					final long id = sourceEntry.getElement().id();
					addToList.accept( id );
//					labelsInBlock.add( id );
					final int searchIndex = list.binarySearch( id );
					if ( list.size() > 0 && searchIndex >= 0 )
						// just add 1 to the count of existing label
						list.get( searchIndex ).setCount( list.get( searchIndex ).getCount() + sourceEntry.getCount() );
					else
					{
						entry.setId( sourceEntry.getElement().id() );
						entry.setCount( sourceEntry.getCount() );
						list.add( entry );
						list.sortById();
					}
				}

				for ( g = 0; g < numDim; g++ )
				{
					totalOffset[ g ] += 1;
					if ( totalOffset[ g ] < cellOffset[ g ] + factor[ g ] && totalOffset[ g ] < maxOffset[ g ] )
						break;
					else
						totalOffset[ g ] = cellOffset[ g ];
				}
			}

			// sort by count and restrict to maxNumEntriesPerPixel (if
			// applicable)
			if ( maxNumEntriesPerPixel > 0 && list.size() > maxNumEntriesPerPixel )
			{
				// change order of e2, e1 for decreasing sort
				list.sort( ( e1, e2 ) -> Long.compare( e2.getCount(), e1.getCount() ) );
				list.limitSize( maxNumEntriesPerPixel );
				list.sortById();
			}

			boolean makeNewList = true;
			final int hash = list.hashCode();
			TIntArrayList offsetsForHash = offsetsForHashes.get( hash );
			if ( offsetsForHash == null )
			{
				offsetsForHash = new TIntArrayList();
				offsetsForHashes.put( hash, offsetsForHash );
			}
			for ( int i = 0; i < offsetsForHash.size(); ++i )
			{
				final int offset = offsetsForHash.get( i );
				list2.referToDataAt( listData, offset );
				if ( list.equals( list2 ) )
				{
					makeNewList = false;
					data[ o++ ] = offset;
					break;
				}
			}
			if ( makeNewList )
			{
				data[ o++ ] = nextListOffset;
				offsetsForHash.add( nextListOffset );
				nextListOffset += list.getSizeInBytes();
			}

			for ( d = 0; d < numDim; d++ )
			{
				cellOffset[ d ] += factor[ d ];
				if ( cellOffset[ d ] < maxOffset[ d ] )
					break;
				else
					cellOffset[ d ] = 0;
			}
		}
		return new VolatileLabelMultisetArray( data, listData, nextListOffset, true, labelsInBlock );
	}

	public static int getSerializedVolatileLabelMultisetArraySize( final VolatileLabelMultisetArray array )
	{
		return ( int ) ( Integer.BYTES + Long.BYTES * array.numContainedLabels() + array.getCurrentStorageArray().length * Integer.BYTES + array.getListDataUsedSizeInBytes() );
	}

	public static void serializeVolatileLabelMultisetArray( final VolatileLabelMultisetArray array, final byte[] bytes )
	{

		final int[] curStorage = array.getCurrentStorageArray();
		final long[] data = ( ( LongMappedAccessData ) array.getListData() ).data;

		final ByteBuffer bb = ByteBuffer.wrap( bytes );

		bb.putInt( array.numContainedLabels() );
		bb.asLongBuffer().put( array.containedLabels() );

		for ( final int d : curStorage )
			bb.putInt( d );

		for ( long i = 0; i < array.getListDataUsedSizeInBytes(); i++ )
			bb.put( ByteUtils.getByte( data, i ) );

	}

}
