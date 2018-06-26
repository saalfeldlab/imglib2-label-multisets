package net.imglib2.type.label;

import java.util.Collection;

import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.RandomAccessibleInterval;

@Deprecated
public class VolatileLabelMultisetArrayCombiner
{
	/**
	 *
	 * @param arrays
	 * @param blockSize
	 * @return
	 */
	public static VolatileLabelMultisetArray combineAndDownsample(
			final RandomAccessibleInterval< LabelMultisetType > data,
			final Collection< VolatileLabelMultisetArray > arrays,
			final int[] downsamplingFactors,
			final int maxNumEntriesPerPixel )
	{

		if ( arrays.size() == 0 ) { return new VolatileLabelMultisetArray( 0, true, new long[] { Label.INVALID } ); }

		if ( arrays.size() == 1 ) { return arrays.iterator().next(); }

		final TLongHashSet labelsInBlock = new TLongHashSet();
		arrays.stream().map( VolatileLabelMultisetArray::containedLabels ).forEach( labelsInBlock::addAll );

		return LabelMultisetTypeDownscaler.createDownscaledCell( data, downsamplingFactors, labelsInBlock, maxNumEntriesPerPixel );

	}
}
