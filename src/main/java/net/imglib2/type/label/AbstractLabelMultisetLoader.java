package net.imglib2.type.label;

import java.nio.ByteBuffer;

import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.label.ByteUtils;
import net.imglib2.type.label.LongMappedAccessData;
import net.imglib2.type.label.VolatileLabelMultisetArray;
import net.imglib2.util.Intervals;

/**
 * A type of {@link CacheLoader} that loads a {@link Cell} of
 * {@link VolatileLabelMultisetArray} from a {@link Long} index and an
 * underlying {@code byte[]}, whose source is determined by subclasses
 *
 * @author Neil Thistlethwaite
 */

// TODO this currently only works because it resides in the same package as LongMappedAccessData (albeit in a different artifact).
// This is very fragile!

public abstract class AbstractLabelMultisetLoader implements CacheLoader< Long, Cell< VolatileLabelMultisetArray > >
{

	protected final CellGrid grid;

	public AbstractLabelMultisetLoader( final CellGrid grid )
	{
		this.grid = grid;
	}

	protected abstract byte[] getData( long... gridPosition );

	@Override
	public Cell< VolatileLabelMultisetArray > get( final Long key )
	{

		final int numDimensions = grid.numDimensions();

		final long[] cellMin = new long[ numDimensions ];
		final int[] cellSize = new int[ numDimensions ];
		final long[] gridPosition = new long[ numDimensions ];
		final int[] cellDimensions = new int[ numDimensions ];

		grid.cellDimensions( cellDimensions );

		grid.getCellDimensions( key, cellMin, cellSize );

		for ( int i = 0; i < numDimensions; ++i )
			gridPosition[ i ] = cellMin[ i ] / cellDimensions[ i ];

		final byte[] bytes = this.getData( gridPosition );

		final ByteBuffer bb = ByteBuffer.wrap( bytes );

		final int[] data = new int[ ( int ) Intervals.numElements( cellSize ) ];
		final int listDataSize = bytes.length - 4 * data.length;
		final LongMappedAccessData listData = LongMappedAccessData.factory.createStorage( listDataSize );

		for ( int i = 0; i < data.length; ++i )
			data[ i ] = bb.getInt();

		for ( int i = 0; i < listDataSize; ++i )
			ByteUtils.putByte( bb.get(), listData.data, i );

		return new Cell<>( cellSize, cellMin, new VolatileLabelMultisetArray( data, listData, true ) );
	}
}
