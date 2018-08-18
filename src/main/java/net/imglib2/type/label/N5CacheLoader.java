package net.imglib2.type.label;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.img.cell.CellGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class N5CacheLoader extends AbstractLabelMultisetLoader
{

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final N5Reader n5;

	private final String dataset;

	private final BiFunction<CellGrid, long[], byte[]> nullReplacement;

	public N5CacheLoader(
			final N5Reader n5,
			final String dataset ) throws IOException
	{
		this( n5, dataset, (g, p) -> null );
	}

	public N5CacheLoader(
			final N5Reader n5,
			final String dataset,
			final BiFunction<CellGrid, long[], byte[]> nullReplacement ) throws IOException
	{
		super( generateCellGrid( n5, dataset ) );
		this.n5 = n5;
		this.dataset = dataset;
		this.nullReplacement = nullReplacement;
	}

	private static CellGrid generateCellGrid( final N5Reader n5, final String dataset ) throws IOException
	{
		final DatasetAttributes attributes = n5.getDatasetAttributes( dataset );

		final long[] dimensions = attributes.getDimensions();
		final int[] cellDimensions = attributes.getBlockSize();

		return new CellGrid( dimensions, cellDimensions );
	}

	@Override
	protected byte[] getData( final long... gridPosition )
	{
		final DataBlock< ? > block;
		try
		{
			LOG.debug( "Reading block for position {}", gridPosition );
			block = n5.readBlock( dataset, n5.getDatasetAttributes( dataset ), gridPosition );
			LOG.debug( "Read block for position {} {}", gridPosition, block );
		}
		catch ( final IOException e )
		{
			LOG.debug( "Caught exception while reading block", e );
			throw new RuntimeException( e );
		}
		return block == null ? nullReplacement.apply( super.grid, gridPosition ): ( byte[] ) block.getData();
	}
}
