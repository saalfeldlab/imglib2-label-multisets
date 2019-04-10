package net.imglib2.type.label;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.IntegerType;

// not thread safe!
public class FromIntegerTypeConverter< I extends IntegerType< I > > implements Converter< I, LabelMultisetType >
{

	public static LongMappedAccessData getListData( final LabelMultisetType l )
	{

		final VolatileLabelMultisetArray access = l.getAccess();
		final LongMappedAccessData listData = ( LongMappedAccessData ) access.getListData();
		return listData;
	}

	public static LabelMultisetType getAppropriateType()
	{
		final LabelMultisetType type = new LabelMultisetType( new VolatileLabelMultisetArray( 1, true, new long[] { Label.INVALID } ) );
		final LongMappedAccessData listData = getListData( type );
		final LongMappedAccess access = listData.createAccess();
		access.putInt( 1, Long.BYTES + Integer.BYTES );
		return type;
	}

	public static VolatileLabelMultisetType getAppropriateVolatileType()
	{
		final VolatileLabelMultisetType type = new VolatileLabelMultisetType( new VolatileLabelMultisetArray( 1, true, new long[] { Label.INVALID } ), true );
		final LongMappedAccessData listData = getListData( type.get() );
		final LongMappedAccess access = listData.createAccess();
		access.putInt( 1, Long.BYTES + Integer.BYTES );
		return type;
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
