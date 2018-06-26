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

	public static LabelMultisetType geAppropriateType()
	{
		final LabelMultisetType type = new LabelMultisetType( new VolatileLabelMultisetArray( 1, true, new long[] { Label.INVALID } ) );
		final LongMappedAccessData listData = getListData( type );
		final LongMappedAccess access = listData.createAccess();
		access.putInt( 1, Long.BYTES + Integer.BYTES );
		return type;
	}

	public static VolatileLabelMultisetType geAppropriateVolatileType()
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
		ByteUtils.putLong( input.getIntegerLong(), getListData( output ).data, Integer.BYTES );
	}

}
