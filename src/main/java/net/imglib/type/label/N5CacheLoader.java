package net.imglib.type.label;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.img.cell.CellGrid;

public class N5CacheLoader extends AbstractLabelMultisetLoader
{
	private final N5Reader n5;

	private final String dataset;

	public N5CacheLoader( final N5Reader n5, final String dataset ) throws IOException
	{
		super( generateCellGrid( n5, dataset ) );
		this.n5 = n5;
		this.dataset = dataset;
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
			block = n5.readBlock( dataset, n5.getDatasetAttributes( dataset ), gridPosition );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
		return ( byte[] ) block.getData();
	}
}
