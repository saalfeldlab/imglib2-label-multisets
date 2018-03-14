package net.imglib2.type.label;

import java.nio.ByteBuffer;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.IterableInterval;
import net.imglib2.type.label.Multiset.Entry;

public class LabelUtils
{

	public static byte[] serializeLabelMultisetTypes(
			final IterableInterval< LabelMultisetType > lmts,
			final int numElements )
	{

		final int[] data = new int[ numElements ];
		final TLongHashSet containedLabels = new TLongHashSet();

		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage( 32 );

		final LabelMultisetEntryList list = new LabelMultisetEntryList( listData, 0 );
		final LabelMultisetEntryList list2 = new LabelMultisetEntryList();
		final TIntArrayList listHashesAndOffsets = new TIntArrayList();
		final LabelMultisetEntry tentry = new LabelMultisetEntry( 0, 1 );
		int nextListOffset = 0;
		int o = 0;
		for ( final LabelMultisetType lmt : lmts )
		{
			list.createListAt( listData, nextListOffset );

			for ( final Entry< Label > entry : lmt.entrySet() )
			{
				final long id = entry.getElement().id();
				containedLabels.add( id );
				tentry.setId( id );
				tentry.setCount( entry.getCount() );
				list.add( tentry );
			}

			boolean makeNewList = true;
			final int hash = list.hashCode();
			for ( int i = 0; i < listHashesAndOffsets.size(); i += 2 )
				if ( hash == listHashesAndOffsets.get( i ) )
				{
					list2.referToDataAt( listData, listHashesAndOffsets.get( i + 1 ) );
					if ( list.equals( list2 ) )
					{
						makeNewList = false;
						data[ o++ ] = listHashesAndOffsets.get( i + 1 );
						break;
					}
				}
			if ( makeNewList )
			{
				data[ o++ ] = nextListOffset;
				listHashesAndOffsets.add( hash );
				listHashesAndOffsets.add( nextListOffset );
				nextListOffset += list.getSizeInBytes();
			}
		}

		final byte[] bytes = new byte[ Integer.BYTES + Long.BYTES * containedLabels.size() + Integer.BYTES * data.length + nextListOffset ];

		final ByteBuffer bb = ByteBuffer.wrap( bytes );
		bb.putInt( containedLabels.size() );
		bb.asLongBuffer().put( containedLabels.toArray() );

		for ( final int d : data )
			bb.putInt( d );

		for ( int i = 0; i < nextListOffset; ++i )
			bb.put( ByteUtils.getByte( listData.data, i ) );

		return bytes;
	}

	public static LabelMultisetType getOutOfBounds()
	{
		return getOutOfBounds( 1 );
	}

	public static LabelMultisetType getOutOfBounds( final int count )
	{

		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage( 32 );

		final LabelMultisetEntryList list = new LabelMultisetEntryList( listData, 0 );
		final LabelMultisetEntry entry = new LabelMultisetEntry( 0, 1 );

		list.createListAt( listData, 0 );
		entry.setId( Label.OUTSIDE );
		entry.setCount( count );
		list.add( entry );

		final int[] data = new int[] { 0 };

		final TLongHashSet containedLabels = new TLongHashSet();
		containedLabels.add( Label.OUTSIDE );

		return new LabelMultisetType( new VolatileLabelMultisetArray( data, listData, true, containedLabels ) );
	}
}
