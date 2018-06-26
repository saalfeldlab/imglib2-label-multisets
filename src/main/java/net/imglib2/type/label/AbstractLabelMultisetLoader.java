package net.imglib2.type.label;

import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
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

	private final long invalidLabel;

	public AbstractLabelMultisetLoader( final CellGrid grid )
	{
		this( grid, Label.INVALID );
	}

	public AbstractLabelMultisetLoader( final CellGrid grid, final long invalidLabel )
	{
		this.grid = grid;
		this.invalidLabel = invalidLabel;
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
		{
			gridPosition[ i ] = cellMin[ i ] / cellDimensions[ i ];
		}

		final byte[] bytes = this.getData( gridPosition );

		final int n = ( int ) Intervals.numElements( cellSize );
		return new Cell<>( cellSize, cellMin, bytes == null ? dummy( n, invalidLabel ) : LabelUtils.fromBytes( bytes, n ) );
	}

	public static int labelsListSizeInBytes( final int numLabels )
	{
		return Long.BYTES * numLabels + Integer.BYTES;
	}

	public static int listOffsetsSizeInBytes( final int numOffsets )
	{
		return Integer.BYTES * numOffsets;
	}

	public static VolatileLabelMultisetArray dummy( final int numElements, final long label )
	{
		final LongMappedAccessData store = LongMappedAccessData.factory.createStorage( 16 );
		final LongMappedAccess access = store.createAccess();
		access.putInt( 1, 0 );
		access.putLong( label, Integer.BYTES );
		access.putInt( 1, Integer.BYTES + Long.BYTES );
		final int[] data = new int[ numElements ];
		final long[] argMax = { label };
		return new VolatileLabelMultisetArray( data, store, true, new TLongHashSet( new long[] { label } ), argMax );
	}
}
