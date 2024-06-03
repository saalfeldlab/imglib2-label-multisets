package net.imglib2.type.label;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.type.Index;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Fraction;

import java.math.BigInteger;

public class VolatileLabelMultisetType
		extends Volatile<LabelMultisetType>
		implements NativeType<VolatileLabelMultisetType>, IntegerType<VolatileLabelMultisetType> {

	public static final VolatileLabelMultisetType type = new VolatileLabelMultisetType();

	// this is the constructor if you want it to read from an array
	public VolatileLabelMultisetType(final NativeImg<?, VolatileLabelMultisetArray> img) {

		super(new LabelMultisetType(img));
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileLabelMultisetType(final VolatileLabelMultisetArray access, final boolean isValid) {

		super(new LabelMultisetType(access), isValid);
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType() {

		super(new LabelMultisetType(), true);
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType(final LabelMultisetEntry entry) {

		super(new LabelMultisetType(entry), true);
	}

	// this is the constructor if you want it to be a variable
	public VolatileLabelMultisetType(final LabelMultisetEntryList entries) {

		super(new LabelMultisetType(entries), true);
	}

	protected VolatileLabelMultisetType(final LabelMultisetType t) {

		super(t, true);
	}

	@Override
	public Fraction getEntitiesPerPixel() {

		return t.getEntitiesPerPixel();
	}

	@Override
	public Index index() {

		return t.index();
	}

	@Override
	public VolatileLabelMultisetType createVariable() {

		return new VolatileLabelMultisetType();
	}

	@Override
	public VolatileLabelMultisetType copy() {

		return new VolatileLabelMultisetType(t.copy());
	}

	@Override
	public void set(final VolatileLabelMultisetType c) {

		throw new UnsupportedOperationException();
	}

	@Override
	public NativeTypeFactory<VolatileLabelMultisetType, ?> getNativeTypeFactory() {

		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileLabelMultisetType duplicateTypeOnSameNativeImg() {

		return new VolatileLabelMultisetType(t.duplicateTypeOnSameNativeImg());
	}

	@Override
	public void updateContainer(final Object c) {

		t.updateContainer(c);
		setValid(t.isValid());
	}

	@Override
	public boolean valueEquals(final VolatileLabelMultisetType other) {

		return isValid() && other.isValid() && t.valueEquals(other.t);
	}

	public static VolatileLabelMultisetType singleEntryWithSingleOccurrence() {

		return singleEntryWithNumOccurrences(1);
	}

	public static VolatileLabelMultisetType singleEntryWithNumOccurrences(final int numOccurrences) {

		return new VolatileLabelMultisetType(new LabelMultisetEntry(Label.INVALID, numOccurrences));
	}

	@Override public int getInteger() {

		return get().getInteger();
	}

	@Override public long getIntegerLong() {

		return get().getIntegerLong();
	}

	@Override public BigInteger getBigInteger() {

		return get().getBigInteger();
	}

	@Override public void setInteger(int f) {
		get().setInteger(f);

	}

	@Override public void setInteger(long f) {
		get().setInteger(f);
	}

	@Override public void setBigInteger(BigInteger b) {
		get().setBigInteger(b);
	}

	@Override public void inc() {
		get().inc();
	}

	@Override public void dec() {
		get().dec();
	}

	@Override public double getMaxValue() {

		return get().getMaxValue();
	}

	@Override public double getMinValue() {

		return get().getMinValue();
	}

	@Override public double getMinIncrement() {

		return get().getMinIncrement();
	}

	@Override public int getBitsPerPixel() {

		return get().getBitsPerPixel();
	}

	@Override public int compareTo(VolatileLabelMultisetType o) {

		return get().compareTo(o.get());
	}

	@Override public double getRealDouble() {

		return get().getRealDouble();
	}

	@Override public float getRealFloat() {

		return get().getRealFloat();
	}

	@Override public double getImaginaryDouble() {

		return get().getImaginaryDouble();
	}

	@Override public float getImaginaryFloat() {

		return get().getImaginaryFloat();
	}

	@Override public void setReal(float f) {
		get().setReal(f);
	}

	@Override public void setReal(double f) {
		get().setReal(f);
	}

	@Override public void setImaginary(float f) {
		get().setImaginary(f);
	}

	@Override public void setImaginary(double f) {
		get().setImaginary(f);
	}

	@Override public void setComplexNumber(float r, float i) {
		get().setComplexNumber(r, i);
	}

	@Override public void setComplexNumber(double r, double i) {
		get().setComplexNumber(r, i);
	}

	@Override public float getPowerFloat() {

		return get().getPowerFloat();
	}

	@Override public double getPowerDouble() {

		return get().getPowerDouble();
	}

	@Override public float getPhaseFloat() {

	 	return get().getPhaseFloat();
	}

	@Override public double getPhaseDouble() {

		return get().getPhaseDouble();
	}

	@Override public void complexConjugate() {
		get().complexConjugate();
	}

 	@Override public void add(VolatileLabelMultisetType c) {

		get().add(c.get());
	}

	@Override public void div(VolatileLabelMultisetType c) {

		get().div(c.get());
	}

	@Override public void mul(VolatileLabelMultisetType c) {

		get().mul(c.get());
	}

	@Override public void mul(float c) {

		get().mul(c);
	}

	@Override public void mul(double c) {

		get().mul(c);
	}

	@Override public void pow(VolatileLabelMultisetType c) {
		get().pow(c.get());
	}

	@Override public void pow(double d) {
		get().pow(d);
	}

	@Override public void setOne() {

		get().setOne();
	}

	@Override public void setZero() {
		get().setZero();
	}

	@Override public void sub(VolatileLabelMultisetType c) {

		get().sub(c.get());
	}
}
