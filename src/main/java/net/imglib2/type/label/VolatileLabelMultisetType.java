package net.imglib2.type.label;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;

public class VolatileLabelMultisetType
		extends Volatile< LabelMultisetType >
		implements NativeType< VolatileLabelMultisetType >
{
	public static final VolatileLabelMultisetType type = new VolatileLabelMultisetType();

	// this is the constructor if you want it to read from an array
	public VolatileLabelMultisetType( final NativeImg< ?, VolatileLabelMultisetArray > img )
	{
		super( new LabelMultisetType( img ) );
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileLabelMultisetType( final VolatileLabelMultisetArray access, final boolean isValid )
	{
		super( new LabelMultisetType( access ), isValid );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType()
	{
		super( new LabelMultisetType(), true );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType( final LabelMultisetEntry entry )
	{
		super( new LabelMultisetType( entry ), true );
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType( final LabelMultisetEntryList entries )
	{
		super( new LabelMultisetType( entries ), true );
	}

	protected VolatileLabelMultisetType( final LabelMultisetType t )
	{
		super( t, true );
	}

	@Override
	public Fraction getEntitiesPerPixel()
	{
		return t.getEntitiesPerPixel();
	}

	@Override
	public void updateIndex( final int i )
	{
		t.updateIndex( i );
	}

	@Override
	public int getIndex()
	{
		return t.getIndex();
	}

	@Override
	public void incIndex()
	{
		t.incIndex();
	}

	@Override
	public void incIndex( final int increment )
	{
		t.incIndex( increment );
	}

	@Override
	public void decIndex()
	{
		t.decIndex();
	}

	@Override
	public void decIndex( final int decrement )
	{
		t.decIndex( decrement );
	}

	@Override
	public VolatileLabelMultisetType createVariable()
	{
		return new VolatileLabelMultisetType();
	}

	@Override
	public VolatileLabelMultisetType copy()
	{
		return new VolatileLabelMultisetType( t.copy() );
	}

	@Override
	public void set( final VolatileLabelMultisetType c )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeTypeFactory< VolatileLabelMultisetType, ? > getNativeTypeFactory()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileLabelMultisetType duplicateTypeOnSameNativeImg()
	{
		return new VolatileLabelMultisetType( t.duplicateTypeOnSameNativeImg() );
	}

	@Override
	public void updateContainer( final Object c )
	{
		t.updateContainer( c );
		setValid( t.isValid() );
	}

	@Override
	public boolean valueEquals( final VolatileLabelMultisetType other )
	{
		return isValid() && other.isValid() && t.valueEquals( other.t );
	}

	public static VolatileLabelMultisetType singleEntryWithSingleOccurrence()
	{
		return singleEntryWithNumOccurrences( 1 );
	}

	public static VolatileLabelMultisetType singleEntryWithNumOccurrences( final int numOccurrences )
	{
		return new VolatileLabelMultisetType( new LabelMultisetEntry( Label.INVALID, numOccurrences ) );
	}
}
