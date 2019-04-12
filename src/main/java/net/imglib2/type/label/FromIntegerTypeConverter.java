package net.imglib2.type.label;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.IntegerType;

// not thread safe!
public class FromIntegerTypeConverter< I extends IntegerType< I > > implements Converter< I, LabelMultisetType >
{
	public static LongMappedAccessData getListData( final LabelMultisetType l )
	{
		final VolatileLabelMultisetArray access = l.getAccess();
		final LongMappedAccessData listData = access.getListData();
		return listData;
	}

	public static LabelMultisetType getAppropriateType()
	{
		return new LabelMultisetType( new LabelMultisetEntry( Label.INVALID, 1 ) );
	}

	public static VolatileLabelMultisetType getAppropriateVolatileType()
	{
		return new VolatileLabelMultisetType( new LabelMultisetEntry( Label.INVALID, 1 ) );
	}

	@Override
	public void convert( final I input, final LabelMultisetType output )
	{
		final long newVal = input.getIntegerLong();
		final long[] data = getListData( output ).data;
		if ( ByteUtils.getLong( data, Integer.BYTES ) != newVal )
		{
			ByteUtils.putLong( newVal, data, Integer.BYTES );
			output.getAccess().setArgMax( output.getIndex(), newVal );
		}
	}
}
